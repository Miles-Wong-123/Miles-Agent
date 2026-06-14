package com.miles.milesagent.Monitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 监控上下文对象。
 * 用来在一次请求链路中携带 userId、sessionId 等标识，方便日志和指标打点。
 */
@Data
@Builder
@AllArgsConstructor
public class MonitorContext implements Serializable {

    /**
     * 当前会话 id。
     */
    private Long sessionId;

    /**
     * 当前用户 id。
     */
    private Long userId;

    @Serial
    private static final long serialVersionUID = 1L;
}
