package com.miles.milesagent.controller;


import com.miles.milesagent.Monitor.MonitorContext;
import com.miles.milesagent.Monitor.MonitorContextHolder;
import com.miles.milesagent.ai.AiChat;
import com.miles.milesagent.model.dto.ChatRequest;
import com.miles.milesagent.model.dto.KnowledgeRequest;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 对外暴露 HTTP 接口的控制器。
 * 可以把它理解成“前端 / 客户端”和 AI 能力之间的第一道入口。
 */
@RestController
@Slf4j
public class AiChatController {

    /**
     * LangChain4j 生成的 AI 对话代理。
     */
    @Resource
    private AiChat aiChat;

    /**
     * 用于把文档切分、向量化并写入向量库的组件。
     */
    @Resource
    private EmbeddingStoreIngestor embeddingStoreIngestor;

    /**
     * 知识库文档的物理目录，来自配置文件中的 rag.docs-path。
     */
    @Value("${rag.docs-path}")
    private String docsPath;

    /**
     * 当调用方没有显式指定文件名时，默认写入这个知识库文档。
     */
    private final String TARGET_FILENAME = "MilesAgent.md";

//    @GetMapping("/chat")
//    public String chat(String sessionId, String prompt) {
//        return aiChat.chat(sessionId, prompt);
//    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest chatRequest) {
        // 在模型调用前，把用户和会话信息放进线程上下文，便于监控模块打点。
        MonitorContextHolder.setContext(MonitorContext.builder().userId(chatRequest.getUserId()).sessionId(chatRequest.getSessionId()).build());

        // 交给 LangChain4j 代理执行一次普通对话。
        String chat = aiChat.chat(chatRequest.getSessionId(), chatRequest.getPrompt());

        // 请求结束后手动清理上下文，避免线程复用时串数据。
        MonitorContextHolder.clearContext();
        return chat;
    }


//    @PostMapping("/streamChat")
//    public Flux<String> streamChat(@RequestBody ChatRequest chatRequest) {
//        return aiChat.streamChat(chatRequest.getSessionId(), chatRequest.getPrompt());
//    }




    @PostMapping("/streamChat")
    public Flux<String> streamChat(@RequestBody ChatRequest chatRequest) {
        // 先构造一份监控上下文，后面在流真正订阅时再放进线程变量。
        MonitorContext context = MonitorContext.builder()
                .userId(chatRequest.getUserId())
                .sessionId(chatRequest.getSessionId())
                .build();

        return Flux.defer(() -> {
            // Flux.defer 的意义是“等真正开始消费流时再执行这里的逻辑”。
            MonitorContextHolder.setContext(context);
            return aiChat.streamChat(chatRequest.getSessionId(), chatRequest.getPrompt())
                    // 无论流正常结束、报错还是取消订阅，都把上下文清掉。
                    .doFinally(signal -> MonitorContextHolder.clearContext());
        });
    }



    @PostMapping("/insert")
    public String insertKnowledge(@RequestBody KnowledgeRequest knowledgeRequest) {
        // 1. 把问题和答案整理成统一的 markdown 片段，便于后续写文件和检索。
        String formattedContent = String.format("### Q：%s\n\nA：%s", knowledgeRequest.getQuestion(), knowledgeRequest.getAnswer());

        // 2. 先写入本地知识文件，确保文本有物理落盘。
        boolean writeSuccess = appendToFile(formattedContent, knowledgeRequest.getSourceName());
        if (!writeSuccess) {
            return "插入失败：无法写入本地文件";
        }

        // 3. 再把同样内容写入向量库，供后续 RAG 检索使用。
        try {
            // 记录“这段知识来自哪个文件”，后续检索结果里就能带上来源。
            String sourceName = (knowledgeRequest.getSourceName() != null) ? knowledgeRequest.getSourceName() : TARGET_FILENAME;
            Metadata metadata = Metadata.from("file_name", sourceName);

            // 包装成 Document 后交给 Ingestor，它会负责切分、向量化和入库。
            Document document = Document.from(formattedContent, metadata);
            embeddingStoreIngestor.ingest(document);

            log.info("RAG - 新增知识点成功: {}", knowledgeRequest.getQuestion());
            return "插入成功：已同步至 " + knowledgeRequest.getSourceName() + " 及向量数据库";
        } catch (Exception e) {
            log.error("RAG - 向量化失败", e);
            return "插入部分成功：文件已写入，但向量库更新失败";
        }
    }



    private synchronized boolean appendToFile(String content, String sourceName) {
        try {
            // 根据配置目录和文件名拼出最终落盘位置。
            Path filePath = Paths.get(docsPath, sourceName);
            log.info("文件实际写入位置: {}", filePath.toAbsolutePath());

            // 文件不存在时，先创建目录，再创建空文件。
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
                Files.createFile(filePath);
            }

            // 追加写入时前面补空行，避免和前一条知识粘在一起。
            String textToAppend = "\n\n" + content;

            // 采用追加模式，不覆盖原有知识库内容。
            Files.writeString(
                    filePath,
                    textToAppend,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE
            );
            return true;
        } catch (IOException e) {
            log.error("RAG - 写入本地文件失败: {}", e.getMessage(), e);
            return false;
        }
    }
}



