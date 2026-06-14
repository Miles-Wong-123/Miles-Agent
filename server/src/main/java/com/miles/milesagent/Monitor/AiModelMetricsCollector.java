package com.miles.milesagent.Monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 指标采集器。
 * 负责把 AI 请求次数、错误次数、Token 消耗和响应耗时写入 Micrometer。
 */
@Component
@Slf4j
public class AiModelMetricsCollector {

    /**
     * Micrometer 指标注册中心，Prometheus 最终会从这里暴露的数据中抓取指标。
     */
    @Resource
    private MeterRegistry meterRegistry;

    // 缓存已经创建好的指标对象，避免每次请求都重复 register。
    private final ConcurrentMap<String, Counter> requestCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> errorCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> tokenCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> responseTimersCache = new ConcurrentHashMap<>();

    /**
     * 记录请求次数。
     */
    public void recordRequest(String userId, String sessionId, String modelName, String status) {
        // Micrometer 的 tag 不能是 null，所以先做兜底。
        String safeUserId = (userId == null) ? "unknown" : userId;
        String safeSessionId = (sessionId == null) ? "unknown" : sessionId;
        String safeModel = (modelName == null) ? "unknown" : modelName;
        String safeStatus = (status == null) ? "unknown" : status;

        // key 只用于本地缓存，不会暴露给 Prometheus。
        String key = String.format("%s_%s_%s_%s", safeUserId, safeSessionId, safeModel, safeStatus);
        Counter counter = requestCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_requests_total")
                        .tag("user_id", safeUserId)
                        .tag("session_id", safeSessionId)
                        .tag("model_name", safeModel)
                        .tag("status", safeStatus)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 记录错误次数。
     */
    public void recordError(String userId, String sessionId, String modelName, String errorMessage) {
        String key = String.format("%s_%s_%s_%s", userId, sessionId, modelName, errorMessage);
        Counter counter = errorCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_errors_total")
                        .description("AI模型错误次数")
                        .tag("user_id", userId)
                        .tag("session_id", sessionId)
                        .tag("model_name", modelName)
                        .tag("error_message", errorMessage)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 记录 Token 消耗。
     */
    public void recordTokenUsage(String userId, String sessionId, String modelName,
                                 String tokenType, long tokenCount) {
        String key = String.format("%s_%s_%s_%s", userId, sessionId, modelName, tokenType);
        Counter counter = tokenCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_tokens_total")
                        .description("AI模型Token消耗总数")
                        .tag("user_id", userId)
                        .tag("session_id", sessionId)
                        .tag("model_name", modelName)
                        .tag("token_type", tokenType)
                        .register(meterRegistry)
        );
        counter.increment(tokenCount);
    }

    /**
     * 记录响应时间。
     */
    public void recordResponseTime(String userId, String sessionId, String modelName, Duration duration) {
        String key = String.format("%s_%s_%s", userId, sessionId, modelName);
        Timer timer = responseTimersCache.computeIfAbsent(key, k ->
                Timer.builder("ai_model_response_duration_seconds")
                        .description("AI模型响应时间")
                        .tag("user_id", userId)
                        .tag("session_id", sessionId)
                        .tag("model_name", modelName)
                        .register(meterRegistry)
        );
        timer.record(duration);
    }
}
