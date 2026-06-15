package com.miles.milesagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miles.milesagent.common.ResultUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * 简单登录拦截器：保护业务接口，放行认证接口、Actuator 与静态资源。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public AuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (handler instanceof ResourceHttpRequestHandler) {
            return true;
        }
        if (isPublicSpaRoute(request.getMethod(), path)) {
            return true;
        }
        if (path.startsWith("/auth/") || path.startsWith("/actuator/") || path.equals("/error")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ResultUtils.error(401, "未登录")));
        return false;
    }

    private boolean isPublicSpaRoute(String method, String path) {
        if (!HttpMethod.GET.matches(method) && !HttpMethod.HEAD.matches(method)) {
            return false;
        }
        return path.equals("/") || path.isEmpty() || path.equals("/login") || path.equals("/register");
    }
}
