package com.example.huffmancode.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // 启用Spring Security的Web安全支持
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // 禁用CSRF保护（适用于API）
                .cors(cors -> {}) // 启用CORS支持
                // 配置URL访问权限
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/huffman/**").permitAll()
                        .requestMatchers("/api/messages/**").permitAll()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    @Bean
    // 使用BCrypt算法进行密码加密（单项哈希算法）
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
