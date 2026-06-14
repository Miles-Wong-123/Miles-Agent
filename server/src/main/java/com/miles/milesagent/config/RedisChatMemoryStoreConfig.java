package com.miles.milesagent.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 聊天记忆配置。
 * 负责把配置文件中的 redis 参数映射成 LangChain4j 可用的存储对象。
 */
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {

    /**
     * Redis 主机。
     */
    private String host;

    /**
     * Redis 端口。
     */
    private int port;

    /**
     * Redis 密码。
     */
    private String password;

    /**
     * 记忆在 Redis 中的过期时间。
     */
    private long ttl;

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        // 创建 Redis 版聊天记忆存储器，后续 MessageWindowChatMemory 会用它读写历史消息。
        return RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                .ttl(ttl)
                .user("default")
                .build();
    }
}
