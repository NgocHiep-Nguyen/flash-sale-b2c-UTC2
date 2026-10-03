package com.b2c.flash_sale_b2c_UTC2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Cấu hình chia sẻ tài nguyên nguồn gốc chéo (CORS) cho phép Storefront Next.js và Admin kết nối.
 *
 * Origins được đọc từ env var {@code cors.allowed-origins} (comma-separated).
 * Mặc định local: http://localhost:3000, http://127.0.0.1:3000
 * Production: set trong application-prod.yaml → biến CORS_ALLOWED_ORIGINS.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private String allowedOriginsRaw;

    private String[] getAllowedOrigins() {
        return Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = Arrays.asList(getAllowedOrigins());

        registry.addMapping("/**")
                .allowedOriginPatterns(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // WebSocket SockJS transport cần riêng CORS config
        // (SockJS dùng iframe + xhr-streaming có thể trigger preflight)
        registry.addMapping("/ws/**")
                .allowedOriginPatterns(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}