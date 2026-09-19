package com.b2c.flash_sale_b2c_UTC2.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình tài liệu hóa Swagger UI & OpenAPI Specification v3.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flash Sale B2C - UTC2 API Documentation")
                        .version("1.0.0")
                        .description("Hệ thống Sàn Flash Sale B2C Chống Over-selling bằng Distributed Lock trên Redis - Trường ĐH Giao thông Vận tải Phân hiệu tại TP.HCM")
                        .contact(new Contact()
                                .name("Đội ngũ phát triển Flash Sale B2C")
                                .email("contact@utc2.edu.vn"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập JWT Token theo định dạng: Bearer <token>")));
    }
}
