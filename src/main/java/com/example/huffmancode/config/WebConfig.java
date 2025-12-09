package com.example.huffmancode. config;

import org.springframework.context. annotation.Configuration;
import org.springframework. web.servlet.config.annotation.CorsRegistry;
import org.springframework.web. servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 对所有api开头的路径启用CORS支持
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        // 也为 WebSocket 添加 CORS 支持
        registry.addMapping("/ws/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}