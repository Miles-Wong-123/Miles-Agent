package com.miles.milesagent.config;

import com.miles.milesagent.Monitor.AiModelMonitorListener;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * 配置大模型客户端。
 * 这里把 DashScope 的同步模型和流式模型都注册成 Spring Bean。
 */
@Configuration
public class DashScopeModelConfig {

    /**
     * DashScope API Key。
     */
    @Value("${langchain4j.community.dashscope.chat-model.api-key}")
    private String apiKey;

    /**
     * 模型名称，例如 qwen-max。
     */
    @Value("${langchain4j.community.dashscope.chat-model.model-name}")
    private String modelName;

    /**
     * 模型调用监听器，用于记录耗时、token 和错误等监控信息。
     */
    @Resource
    private AiModelMonitorListener aiModelMonitorListener;

    @Bean
    @Primary
    public ChatModel chatModel() {
        // 构造同步调用模型，并挂上监控监听器。
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .listeners(List.of(aiModelMonitorListener))
                .build();
    }

    @Bean
    @Primary
    public StreamingChatModel streamingChatModel() {
        // 构造流式模型，让前端可以边生成边展示结果。
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .listeners(List.of(aiModelMonitorListener))
                .build();
    }
}
