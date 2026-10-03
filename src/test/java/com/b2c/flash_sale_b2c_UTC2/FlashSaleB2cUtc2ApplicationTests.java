package com.b2c.flash_sale_b2c_UTC2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration",
        "jwt.secret=MockSecureSecretForTestingJwtPropertiesValidation2026"
})
class FlashSaleB2cUtc2ApplicationTests {

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory redisConnectionFactory;

    @Test
    void contextLoads() {
    }

}
