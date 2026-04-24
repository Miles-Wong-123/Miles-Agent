package com.miles.milesagent.config;


import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG（检索增强生成）相关配置。
 * 这里负责两件事：
 * 1. 定义“文档如何切分并写入向量库”
 * 2. 定义“问答前如何从向量库检索相关内容”
 */
@Configuration
@SuppressWarnings({"all"})
public class RagConfig {

    /**
     * 文本向量化模型。
     */
    @Resource
    private EmbeddingModel embeddingModel;

    /**
     * 向量库存储对象。
     */
    @Resource(name = "initEmbeddingStore")
    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 本地知识文档目录。
     * 当前类没有直接使用它做逻辑，只是保留了配置项接入。
     */
    @Value("${rag.docs-path}")
    private String docsPath;

    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor() {
        // 按段落切分文档：每段最多 300 字符，段落间允许 100 字符重叠。
        DocumentByParagraphSplitter paragraphSplitter = new DocumentByParagraphSplitter(300, 100);

        return EmbeddingStoreIngestor.builder()
                .documentSplitter(paragraphSplitter)
                // 在每段文本前加上文件名，提升检索结果的可解释性。
                .textSegmentTransformer(textSegment -> TextSegment.from(
                        textSegment.metadata().getString("file_name") + "\n" + textSegment.text(),
                        textSegment.metadata()
                ))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }



    @Bean
    public ContentRetriever contentRetriever() {
        // 这里定义问答时的向量检索策略：
        // 1. 先把用户问题向量化
        // 2. 在向量库里找最相近的片段
        // 3. 只保留分数足够高的结果，避免把噪声传给模型
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.75)
                .build();

        // 这里还没有接入 rerank 或更复杂的召回策略，先返回基础检索器。
        return contentRetriever;
    }
}
