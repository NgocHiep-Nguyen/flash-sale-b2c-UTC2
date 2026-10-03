package com.b2c.flash_sale_b2c_UTC2.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Khóa bí mật ký token HMAC-SHA256 (tối thiểu 256 bits = 32 ký tự).
     */
    private String secret;

    /**
     * Thời gian sống của Access Token (milliseconds), mặc định 1 giờ = 3,600,000ms.
     */
    private long expirationMs = 3600000L;

    /**
     * Thời gian sống của Refresh Token (milliseconds), mặc định 7 ngày = 604,800,000ms.
     */
    private long refreshExpirationMs = 604800000L;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Cấu hình bảo mật lỗi: 'jwt.secret' không được để trống! Vui lòng cấu hình biến môi trường JWT_SECRET.");
        }
        if (secret.trim().length() < 32) {
            throw new IllegalStateException("Cấu hình bảo mật lỗi: 'jwt.secret' quá ngắn! Cần tối thiểu 32 ký tự (256-bit) cho thuật toán HMAC-SHA256.");
        }
    }
}
