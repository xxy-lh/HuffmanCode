package com.example.huffmancode.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 允许/api/下的所有路径
                .allowedOrigins("http://localhost:5173") // 明确允许前端应用的源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许所有常用方法
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(true); // 如果需要，允许凭证
    }
}
