package com.miles.milesagent.common;

/**
 * 返回对象构造工具类。
 * 作用是减少 controller / service 手写 BaseResponse 的重复代码。
 */
public class ResultUtils {

    /**
     * 构造成功响应。
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(200, data, "ok");
    }

    /**
     * 按错误码枚举构造失败响应。
     */
    public static BaseResponse error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 按自定义 code + message 构造失败响应。
     */
    public static BaseResponse error(int code, String message) {
        return new BaseResponse(code, null, message);
    }

    /**
     * 用指定错误码枚举，但覆盖 message。
     */
    public static BaseResponse error(ErrorCode errorCode, String message) {
        return new BaseResponse(errorCode.getCode(), null, message);
    }
}
