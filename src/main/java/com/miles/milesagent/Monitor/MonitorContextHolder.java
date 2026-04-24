package com.miles.milesagent.Monitor;

import lombok.extern.slf4j.Slf4j;

/**
 * 监控上下文持有器。
 * 底层使用 ThreadLocal，让同一线程里的不同代码都能拿到当前请求的监控信息。
 */
@Slf4j
public class MonitorContextHolder {

    /**
     * InheritableThreadLocal 允许子线程继承父线程上下文，便于异步场景透传。
     */
    private static final ThreadLocal<MonitorContext> CONTEXT_HOLDER = new InheritableThreadLocal<>();

    /**
     * 设置监控上下文。
     */
    public static void setContext(MonitorContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前线程里的监控上下文。
     */
    public static MonitorContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除当前线程里的监控上下文，防止线程复用导致串数据。
     */
    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }
}
