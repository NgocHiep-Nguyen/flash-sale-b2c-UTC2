package com.b2c.flash_sale_b2c_UTC2.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình RedissonClient phục vụ chiến lược khóa phân tán (RedissonLockStrategy)
 * Lettuce vẫn là client mặc định cho RedisTemplate và Lua scripts.
 */
import org.springframework.context.annotation.Lazy;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Lazy
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "redisson.enabled", havingValue = "true")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        var singleServerConfig = config.useSingleServer().setAddress(address);
        if (redisPassword != null && !redisPassword.isBlank()) {
            singleServerConfig.setPassword(redisPassword);
        }
        return Redisson.create(config);
    }
}
