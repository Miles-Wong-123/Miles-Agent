package com.miles.milesagent.job;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 应用启动后自动把本地知识文档灌入向量库。
 * 由于实现了 CommandLineRunner，所以 Spring Boot 启动完成后会自动执行 run 方法。
 */
@Component
@Slf4j
public class RagDataLoader implements CommandLineRunner {

    /**
     * 本地知识库目录。
     */
    @Value("${rag.docs-path}")
    private String docsPath;

    /**
     * 文档写入向量库的执行器。
     */
    @Resource
    private EmbeddingStoreIngestor embeddingStoreIngestor;

    @Override
    public void run(String... args) {
        log.info("RAG - 开始加载本地基础文档，路径: {}", docsPath);
        try {
            // 扫描目录下的所有文档，并封装成 Document 列表。
            List<Document> documents = FileSystemDocumentLoader.loadDocuments(docsPath);

            if (!documents.isEmpty()) {
                // 一次性导入，内部会完成切分、向量化和存储。
                embeddingStoreIngestor.ingest(documents);
                log.info("RAG - 本地文档加载完成，共加载 {} 个文档", documents.size());
            } else {
                log.warn("RAG - 指定路径下未发现文档");
            }
        } catch (Exception e) {
            log.error("RAG - 加载本地文档失败", e);
        }
    }
}
