package com.miles.milesagent.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.Set;

/**
 * 输入防护栏。
 * 在请求真正进入模型前，先检查用户输入里是否包含不允许的关键词。
 */
public class SafeInputGuardrail implements InputGuardrail {

    /**
     * 当前项目里定义的敏感词集合。
     * 这里只是非常基础的演示版，正式项目通常会更复杂。
     */
    private static final Set<String> sensitiveWords = Set.of("死", "杀");

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        // 把用户消息取成纯文本，后面做关键词匹配。
        String inputText = userMessage.singleText();

        // 命中任一敏感词就直接终止请求，不再放给模型处理。
        for (String keyword : sensitiveWords) {
            if (!keyword.isEmpty() && inputText.contains(keyword)) {
                return fatal("提问不能包含敏感词！！！！！");
            }
        }

        // 没命中则放行。
        return success();
    }
}
