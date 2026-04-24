package com.miles.milesagent.model.dto;

import lombok.Data;

/**
 * 聊天请求体。
 * 前端调用 /chat 或 /streamChat 时，会把 JSON 反序列化成这个对象。
 */
@Data
public class ChatRequest {

    /**
     * 会话 id。
     * 用来区分不同聊天上下文，同时也是 Redis 记忆的 key。
     */
    private Long sessionId;

    /**
     * 用户 id。
     * 当前主要用于监控打点和日志定位。
     */
    private Long userId;

    /**
     * 用户本次输入的问题或指令。
     */
    private String prompt;
}
