package com.b2c.flash_sale_b2c_UTC2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình bảo mật nền tảng (Security Skeleton Configuration).
 *
 * Ghi chú: Trong giai đoạn skeleton, cấu hình mở quyền cho tài liệu Swagger/OpenAPI và các API
 * để các thành viên trong nhóm có thể phát triển và kiểm thử mà không bị chặn xác thực cơ bản.
 * Tầng Authentication/JWT/RBAC sẽ được tích hợp hoàn thiện trong giai đoạn Implementation.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập Swagger UI và OpenAPI Docs
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/error"
                        ).permitAll()
                        // Skeleton phase: Mở quyền cho các API thử nghiệm trong lúc team triển khai
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
