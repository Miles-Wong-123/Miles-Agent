package com.miles.milesagent.Exception;


import com.miles.milesagent.common.ErrorCode;

/**
 * 抛异常工具类。
 * 用来把 if 判断 + throw 的模板代码收敛掉。
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     *
     * @param condition        是否满足抛异常条件
     * @param runtimeException 要抛出的运行时异常
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 是否满足抛异常条件
     * @param errorCode 错误码枚举
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 是否满足抛异常条件
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
