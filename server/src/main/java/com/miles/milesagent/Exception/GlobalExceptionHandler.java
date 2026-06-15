package com.miles.milesagent.Exception;


import com.miles.milesagent.common.BaseResponse;
import com.miles.milesagent.common.ErrorCode;
import com.miles.milesagent.common.ResultUtils;
import dev.langchain4j.guardrail.InputGuardrailException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器。
 * 所有 controller 里抛出的异常，只要命中这里的 @ExceptionHandler，就会被统一转换成响应。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> businessExceptionHandler(BusinessException e) {
        HttpStatus status = mapStatus(e.getCode());
        if (status.is5xxServerError()) {
            log.error("BusinessException", e);
        } else {
            log.warn("BusinessException: code={}, message={}", e.getCode(), e.getMessage());
        }
        return ResponseEntity.status(status)
                .body(ResultUtils.error(e.getCode(), e.getMessage()));
    }

    /**
     * 处理没有被更细粒度捕获的运行时异常。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse<?>> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误"));
    }

    /**
     * 处理 @Valid 等参数校验失败场景。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });
        String message = errors.toString();
        return ResponseEntity.badRequest().body(ResultUtils.error(ErrorCode.INVALID_PARAMETER_ERROR, message));
    }


    /**
     * 处理缺少请求参数的情况。
     */
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse<?>> handlerMissingServletRequestParameterException(Exception e) {
        log.error("缺少必填参数:{}", e.toString());
        return ResponseEntity.badRequest().body(ResultUtils.error(ErrorCode.INVALID_PARAMETER_ERROR, "缺少必填参数"));
    }

    /**
     * 处理输入防护栏拦截的异常。
     */
    @ExceptionHandler(InputGuardrailException.class)
    public ResponseEntity<BaseResponse<?>> inputGuardrailExceptionHandler(InputGuardrailException e) {
        log.error("敏感词拦截: {}", e.getMessage());
        // 当前统一返回固定错误码，而不是直接把底层异常原文透给前端。
        return ResponseEntity.badRequest().body(ResultUtils.error(ErrorCode.SENSITIVE_WORD_ERROR));
    }

    private HttpStatus mapStatus(int code) {
        if (code == 401 || (code >= 40100 && code < 40200)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code >= 40300 && code < 40400) {
            return HttpStatus.FORBIDDEN;
        }
        if (code >= 40400 && code < 40500) {
            return HttpStatus.NOT_FOUND;
        }
        if (code >= 50000 && code < 60000) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
