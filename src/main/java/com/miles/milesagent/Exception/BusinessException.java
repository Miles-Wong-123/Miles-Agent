package com.miles.milesagent.Exception;


import com.miles.milesagent.common.ErrorCode;

/**
 * 自定义业务异常。
 * 当代码想带着明确的错误码和错误消息向上抛出时，可以使用这个异常。
 */
public class BusinessException extends RuntimeException {

    /**
     * 与 ErrorCode 对应的业务错误码。
     */
    private final int code;

    /**
     * 直接传入错误码和消息。
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 直接根据 ErrorCode 构造异常。
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用 ErrorCode 的 code，但允许覆盖 message。
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 获取业务错误码。
     */
    public int getCode() {
        return code;
    }
}
