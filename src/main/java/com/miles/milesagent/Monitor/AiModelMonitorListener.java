package com.miles.milesagent.Monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;


/**
 * 模型监听器。
 * 每次模型发请求、收到响应、发生错误时，都会经过这里，从而完成日志记录和指标打点。
 */
@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {

    // 在 request 和 response 之间传递开始时间，用来计算耗时。
    private static final String START_TIME_KEY = "request_start_time";

    // 在 request 和 response 之间传递监控上下文，避免后续阶段拿不到 userId/sessionId。
    private static final String MONITOR_CONTEXT_KEY = "monitor_context";

    /**
     * 真正负责写 Micrometer 指标的组件。
     */
    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;


    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // 记录请求开始时间，响应阶段要用它计算耗时。
        requestContext.attributes().put(START_TIME_KEY, Instant.now());

        // 从当前线程里取出 controller 预先放进去的 userId/sessionId。
        MonitorContext context = MonitorContextHolder.getContext();

        if (context == null) {
            // 正常情况下这里不应该为空；如果为空，说明上下文透传链断了。
            log.error("MonitorContext is null when processing request");
            return;
        }
        String userId = context.getUserId() != null ? context.getUserId().toString() : "unknown";
        String sessionId = context.getSessionId() != null ? context.getSessionId().toString() : "unknown";

        // 存进 attributes，便于 onResponse / onError 阶段继续读取。
        requestContext.attributes().put(MONITOR_CONTEXT_KEY, context);

        // 当前实际调用的模型名。
        String modelName = requestContext.chatRequest().modelName();

        log.info(">>> AI请求开始 | 用户: {} | 会话: {} | 模型: {}", userId, sessionId, modelName);
        // 标记一次“started”请求。
        aiModelMetricsCollector.recordRequest(userId, sessionId, modelName, "started");
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        String modelName = responseContext.chatResponse().metadata().modelName();

        // 读取 request 阶段保存的 attributes。
        Map<Object, Object> attributes = responseContext.attributes();

        // 1. 拿到 userId / sessionId。
        MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);

        if (context == null) {
            log.warn("监控上下文丢失，无法记录响应指标 - Model: {}", responseContext.chatResponse().modelName());
            return;
        }
        
        String userId = context.getUserId().toString();
        String sessionId = context.getSessionId().toString();

        // 2. 计算本次调用耗时。
        Duration durationMs = calculateDuration(attributes);

        // 3. 读取 token 使用情况。
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();

        // 4. 打日志并写指标。
        log.info("<<< AI请求成功 | 用户: {} | 会话: {} | 模型: {} | 耗时: {}ms | Tokens: [In:{}, Out:{}, Total:{}]", userId, sessionId, modelName, durationMs.toMillis(), tokenUsage != null ? tokenUsage.inputTokenCount() : 0, tokenUsage != null ? tokenUsage.outputTokenCount() : 0, tokenUsage != null ? tokenUsage.totalTokenCount() : 0);
        aiModelMetricsCollector.recordRequest(userId, sessionId, modelName, "success");
        aiModelMetricsCollector.recordResponseTime(userId, sessionId, modelName, durationMs);

        if (tokenUsage != null) {
            // token 分输入、输出、总量三类分别累计。
            aiModelMetricsCollector.recordTokenUsage(userId, sessionId, modelName, "input", tokenUsage.inputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, sessionId, modelName, "output", tokenUsage.outputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, sessionId, modelName, "total", tokenUsage.totalTokenCount());
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        // 优先从当前线程取上下文。
        MonitorContext context = MonitorContextHolder.getContext();

        Map<Object, Object> attributes = errorContext.attributes();
        Duration durationMs = calculateDuration(attributes);

        if (context == null) {
            // 如果线程上下文已经丢了，再尝试从 attributes 补救。
            context = (MonitorContext) errorContext.attributes().get(MONITOR_CONTEXT_KEY);
        }

        if (context == null) {
            log.warn("监控上下文丢失，无法记录错误指标 - Error: {}", errorContext.error().getMessage());
            return;
        }
        
        String userId = context.getUserId().toString();
        String sessionId = context.getSessionId().toString();
        String modelName = errorContext.chatRequest().modelName();
        String errorMessage = errorContext.error().getMessage();
        log.error("AI 请求失败 | 耗时: {}ms | 错误原因: {}", durationMs.toMillis(), errorContext.error().getMessage());

        // 把错误次数、错误信息和耗时都记录下来。
        aiModelMetricsCollector.recordRequest(userId, sessionId, modelName, "error");
        aiModelMetricsCollector.recordError(userId, sessionId, modelName, errorMessage);
        aiModelMetricsCollector.recordResponseTime(userId, sessionId, modelName, durationMs);
    }

    /**
     * 从 attributes 中取出开始时间并计算耗时。
     */
    private Duration calculateDuration(Map<Object, Object> attributes) {
        Instant startTime = (Instant) attributes.get(START_TIME_KEY);
        if (startTime != null) {
            return Duration.between(startTime, Instant.now());
        }
        return Duration.ZERO;
    }
}
