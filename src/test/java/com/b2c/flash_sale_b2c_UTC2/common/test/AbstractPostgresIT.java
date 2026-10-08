package com.b2c.flash_sale_b2c_UTC2.common.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Base class cho Integration Test dùng PostgreSQL local (container pg-test đang chạy trên port 5432).
 *
 * Dùng Postgres thật thay cho H2 vì:
 * - Product.tier_variation_configs / ProductVariant.attributes dùng jsonb (H2 không hỗ trợ)
 * - Test nghiệp vụ thật với Flyway migration
 *
 * Redis bean được mock qua @MockitoBean để không cần Redis thật khi chỉ test DB.
 * TestCorsConfig cung cấp CorsConfigurationSource cho SecurityConfig.
 *
 * Điều kiện tiên quyết: docker run -d --name pg-test -e POSTGRES_DB=flash_sale_test
 *   -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=root -p 5432:5432 postgres:16-alpine
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestCorsConfig.class)
@ActiveProfiles("test-local")
public abstract class AbstractPostgresIT {

    /**
     * Mock Redis beans để các service có thể inject mà không cần Redis thật.
     * Khi test nào cần Redis thật thì ghi đè bằng @TestConfiguration riêng.
     */
    @MockitoBean
    private org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private org.springframework.data.redis.core.RedisTemplate<Object, Object> redisTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/flash_sale_test");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "root");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");
        registry.add("jwt.secret", () -> "TestcontainersSecureKeyWithAtLeast32CharactersHS256Bit2026");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,"
                        + "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
                        + "org.redisson.spring.starter.RedissonAutoConfigurationV2,"
                        + "org.redisson.spring.starter.RedissonAutoConfiguration");
    }
}