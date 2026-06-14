package com.miles.milesagent.tool;

import dev.langchain4j.agent.tool.Tool;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 一个非常简单的时间工具。
 * 模型如果需要知道当前时间，可以主动调用这个工具，而不是自己“猜时间”。
 */
public class TimeTool {

    @Tool("getCurrentTime")
    public String getCurrentTimeInShanghai() {
        // 固定按中国标准时间输出，保证回复里的时间来源一致。
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE (中国标准时间)"));
    }
}
