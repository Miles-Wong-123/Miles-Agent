package com.miles.milesagent.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用返回类
 * 适合给前端返回统一的 code / data / message 结构。
 */
@Data
@NoArgsConstructor
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    /**
     * 完整构造器。
     */
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /**
     * 省略 message 的构造器。
     */
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    /**
     * 直接根据错误码枚举构造失败响应。
     */
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
