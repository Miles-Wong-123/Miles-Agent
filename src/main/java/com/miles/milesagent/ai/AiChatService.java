package com.miles.milesagent.ai;

import com.miles.milesagent.tool.EmailTool;
import com.miles.milesagent.tool.RagTool;
import com.miles.milesagent.tool.TimeTool;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 负责把“模型、记忆、RAG、工具”组装成一个可直接调用的 AI 代理。
 * 这里是整个 Agent 能力拼装的核心位置。
 */
@Configuration
public class AiChatService {

    /**
     * 同步聊天模型。
     */
    @Resource
    private ChatModel chatModel;

    /**
     * 通过 MCP 协议接入的外部工具提供器。
     */
    @Resource
    private McpToolProvider mcpToolProvider;

    /**
     * Redis 聊天记忆存储。
     */
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * RAG 检索器，用于在对话前找相关知识片段。
     */
    @Resource
    private ContentRetriever contentRetriever;

    /**
     * 模型可调用的“写入知识库”工具。
     */
    @Resource
    private RagTool ragTool;

    /**
     * 模型可调用的“发送邮件”工具。
     */
    @Resource
    private EmailTool emailTool;

    /**
     * 流式输出模型。
     */
    @Resource
    private StreamingChatModel streamingChatModel;

    @Bean
    public AiChat aiChat() {
        // AiServices.builder 会根据 AiChat 接口定义，动态生成实现类。
        return AiServices.builder(AiChat.class)
                // 普通问答走这个模型。
                .chatModel(chatModel)
                // 流式问答走这个模型。
                .streamingChatModel(streamingChatModel)
                // 对话前先进行知识检索，把命中的片段一起喂给模型。
                .contentRetriever(contentRetriever)
                // 用 sessionId 作为 memoryId，把最近 20 条消息持久化在 Redis 中。
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory
                        .builder()
                        .id(memoryId)
                        .chatMemoryStore(redisChatMemoryStore)
                        .maxMessages(20)
                        .build())
                // 注册本地 Java 工具，模型在需要时可以自动调用。
                .tools(new TimeTool(), ragTool, emailTool)
                // 注册外部 MCP 工具，例如联网搜索。
                .toolProvider(mcpToolProvider)
                .build();
    }

}
