package com.miles.milesagent.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量库配置。
 * 当前项目使用 pgvector 作为 Embedding 的持久化存储。
 */
@Configuration
@RequiredArgsConstructor
public class EmbeddingStoreConfig {

    /**
     * pgvector 所在主机。
     */
    @Value("${pgvector.host}")
    private String host;

    /**
     * pgvector 端口。
     */
    @Value("${pgvector.port}")
    private int port;

    /**
     * 数据库名称。
     */
    @Value("${pgvector.database}")
    private String database;

    /**
     * 数据库用户名。
     */
    @Value("${pgvector.user}")
    private String user;

    /**
     * 数据库密码。
     */
    @Value("${pgvector.password}")
    private String password;

    /**
     * 向量表名。
     */
    @Value("${pgvector.table}")
    private String table;

    @Bean
    public EmbeddingStore<TextSegment> initEmbeddingStore() {
        // 创建 pgvector 存储对象。
        // 这里设置了 createTable(true)，首次启动时会自动建表。
        // dropTableFirst(true) 表示每次启动会先删旧表再重建，适合演示，不适合正式长期积累数据。
        return PgVectorEmbeddingStore.builder()
                .table(table)
                .dropTableFirst(true)
                .createTable(true)
                .host(host)
                .port(port)
                .user(user)
                .password(password)
                .dimension(1024)
                .database(database)
                .build();
    }
}

