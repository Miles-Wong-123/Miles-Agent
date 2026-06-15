package com.miles.milesagent.controller;

import com.miles.milesagent.auth.dto.AuthResponse;
import com.miles.milesagent.auth.dto.LoginRequest;
import com.miles.milesagent.auth.dto.OkResponse;
import com.miles.milesagent.auth.dto.RegisterRequest;
import com.miles.milesagent.auth.dto.SendCodeRequest;
import com.miles.milesagent.auth.dto.VerifyCodeRequest;
import com.miles.milesagent.auth.service.AuthService;
import com.miles.milesagent.common.BaseResponse;
import com.miles.milesagent.common.ResultUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关接口。
 */
@RestController
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/auth/sendCode")
    public BaseResponse<OkResponse> sendCode(@RequestBody SendCodeRequest request) {
        authService.sendCode(request.getEmail());
        return ResultUtils.success(new OkResponse(true));
    }

    @PostMapping("/auth/verifyCode")
    public BaseResponse<OkResponse> verifyCode(@RequestBody VerifyCodeRequest request) {
        authService.verifyCode(request.getEmail(), request.getCode());
        return ResultUtils.success(new OkResponse(true));
    }

    @PostMapping("/auth/register")
    public BaseResponse<AuthResponse> register(@RequestBody RegisterRequest request, HttpSession session) {
        AuthResponse response = authService.register(request, session);
        authService.bindSessionSnapshot(session, response);
        return ResultUtils.success(response);
    }

    @PostMapping("/auth/login")
    public BaseResponse<AuthResponse> login(@RequestBody LoginRequest request, HttpSession session) {
        AuthResponse response = authService.login(request, session);
        authService.bindSessionSnapshot(session, response);
        return ResultUtils.success(response);
    }

    @PostMapping("/auth/logout")
    public BaseResponse<OkResponse> logout(HttpSession session) {
        authService.logout(session);
        return ResultUtils.success(new OkResponse(true));
    }

    @GetMapping("/auth/me")
    public BaseResponse<AuthResponse> me(HttpSession session) {
        return ResultUtils.success(authService.me(session));
    }
}
