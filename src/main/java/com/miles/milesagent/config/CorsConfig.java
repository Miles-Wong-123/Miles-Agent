package com.miles.milesagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置。
 * 目的是让本地 HTML 页面或其他前端域名可以直接调用这个后端接口。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 对所有路径都开启跨域。
        registry.addMapping("/**")
                // 允许浏览器携带 Cookie / 认证信息。
                .allowCredentials(true)
                // 允许所有来源；因为 allowCredentials=true，所以这里必须用 patterns，不能直接写 "*"
                .allowedOriginPatterns("*")
                // 放开常用 HTTP 方法。
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许任意请求头。
                .allowedHeaders("*")
                // 允许前端读取任意响应头。
                .exposedHeaders("*");
    }
}
