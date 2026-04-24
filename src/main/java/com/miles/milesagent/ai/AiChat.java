package com.miles.milesagent.ai;

import com.miles.milesagent.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;


/**
 * 用接口描述“AI 可以提供哪些对话能力”。
 * LangChain4j 会读取这里的注解并在运行时生成实现类。
 */
@InputGuardrails({SafeInputGuardrail.class})
public interface AiChat {

    /**
     * 普通同步对话接口。
     *
     * @param sessionId 会话 id，同时也会作为记忆的 key
     * @param prompt    用户输入
     * @return 模型返回的完整文本
     */
    @SystemMessage(fromResource = "system-prompt/chat-bot.txt")
    String chat(@MemoryId Long sessionId, @UserMessage String prompt);


    /**
     * 流式对话接口。
     *
     * @param sessionId 会话 id，同时也会作为记忆的 key
     * @param prompt    用户输入
     * @return 按片段持续输出的文本流
     */
    @SystemMessage(fromResource = "system-prompt/chat-bot.txt")
    Flux<String> streamChat(@MemoryId Long sessionId, @UserMessage String prompt);
}
