package com.miles.milesagent.tool;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 提供给大模型调用的知识库写入工具。
 * 当模型判断“用户想把某条问答保存成知识”时，就可以自动调用这个工具。
 */
@Component
@Slf4j
public class RagTool {

    /**
     * 负责把文档写入向量库。
     */
    @Resource
    private EmbeddingStoreIngestor embeddingStoreIngestor;

    /**
     * 知识文档所在目录。
     */
    @Value("${rag.docs-path}")
    private String docsPath;

    /**
     * 定义工具方法。
     * 大模型会根据 @Tool 的描述和参数名来决定何时调用。
     */
    @Tool("当用户想要保存问答对、知识点或者向知识库添加新信息时调用此工具。将问题、答案和目标文件名作为参数。")
    public String addKnowledgeToRag(String question, String answer, String fileName) {
        log.info("Tool 调用: 正在保存知识 - Q: {}, file: {}", question, fileName);

        // 1. 统一格式化成 markdown 问答片段。
        String formattedContent = String.format("### Q：%s\n\nA：%s", question, answer);

        // 2. 修正文件名，保证至少有默认文件名且带 .md 后缀。
        if (fileName == null || fileName.isBlank()) {
            fileName = "MilesAgent.md";
        }
        if (!fileName.endsWith(".md")) {
            fileName = fileName + ".md";
        }

        // 3. 先把文本真实写入磁盘。
        boolean writeSuccess = appendToFile(formattedContent, fileName);
        if (!writeSuccess) {
            return "保存失败：无法写入本地文件系统，请检查日志。";
        }

        // 4. 再把同一份内容写入向量数据库，供后续 RAG 检索使用。
        try {
            // 元数据里保留来源文件名，便于后续检索时知道答案从哪份文档来。
            Metadata metadata = Metadata.from("file_name", fileName);

            // ingest 会负责切分、生成向量并落库。
            Document document = Document.from(formattedContent, metadata);
            embeddingStoreIngestor.ingest(document);

            log.info("Tool 执行成功: 知识已同步至 RAG");
            return "成功！已将该知识点保存到文档 [" + fileName + "] 并同步至向量数据库。";
        } catch (Exception e) {
            log.error("RAG - 向量化失败", e);
            return "文件写入成功，但向量数据库更新失败：" + e.getMessage();
        }
    }

    /**
     * 辅助方法：把知识追加写入到 markdown 文件。
     */
    private synchronized boolean appendToFile(String content, String fileName) {
        try {
            Path filePath = Paths.get(docsPath, fileName);
            
            // 文件不存在时先补目录，再创建文件。
            if (!Files.exists(filePath)) {
                if (filePath.getParent() != null) {
                    Files.createDirectories(filePath.getParent());
                }
                Files.createFile(filePath);
                log.info("Tool created new file: {}", filePath.toAbsolutePath());
            }

            // 前面补空行，防止多条知识写在一起难以阅读。
            String textToAppend = "\n\n" + content;

            // 使用追加模式，避免覆盖之前已经保存的知识。
            Files.writeString(
                    filePath,
                    textToAppend,
                    StandardOpenOption.APPEND
            );
            return true;
        } catch (IOException e) {
            log.error("RAG Tool - 写入文件失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
