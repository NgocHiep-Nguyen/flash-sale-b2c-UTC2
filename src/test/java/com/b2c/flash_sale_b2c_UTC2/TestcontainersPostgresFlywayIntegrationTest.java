package com.b2c.flash_sale_b2c_UTC2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiá»ƒm thá»­ tÃ­ch há»£p khá»Ÿi Ä‘á»™ng PostgreSQL Testcontainers tháº­t vá»›i Flyway migration
 * vÃ  kiá»ƒm chá»©ng tÃ­nh toÃ n váº¹n Hibernate ddl-auto: validate trÃªn schema váº­t lÃ½.
 * Tá»± Ä‘á»™ng bá» qua khi mÃ´i trÆ°á»ng mÃ¡y phÃ¡t triá»ƒn chÆ°a báº­t Docker Desktop daemon.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class TestcontainersPostgresFlywayIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (isDockerAvailable()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.flyway.enabled", () -> "true");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            registry.add("jwt.secret", () -> "TestcontainersSecureKeyWithAtLeast32CharactersHS256Bit2026");
            registry.add("spring.autoconfigure.exclude", () -> "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration");
        }
    }

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Autowired(required = false)
    private DataSource dataSource;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Test
    @EnabledIf("isDockerAvailable")
    @DisplayName("Khá»Ÿi cháº¡y Flyway vÃ  xÃ¡c thá»±c ddl-auto: validate trÃªn PostgreSQL Testcontainers tháº­t")
    void testFlywayAndHibernateSchemaValidationOnRealPostgres() throws Exception {
        assertNotNull(dataSource, "DataSource pháº£i Ä‘Æ°á»£c khá»Ÿi táº¡o");
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(2), "Káº¿t ná»‘i Ä‘áº¿n PostgreSQL container pháº£i há»£p lá»‡");
        }
    }
}
