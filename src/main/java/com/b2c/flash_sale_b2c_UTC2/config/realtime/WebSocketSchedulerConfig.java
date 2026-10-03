package com.b2c.flash_sale_b2c_UTC2.config.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Cấu hình riêng cho WebSocket subsystem: thread pool cho heart-beat
 * và future scalability.
 */
@Configuration
public class WebSocketSchedulerConfig {

    @Value("${websocket.scheduler.pool-size:2}")
    private int poolSize;

    @Value("${websocket.scheduler.thread-name-prefix:ws-scheduler-}")
    private String threadNamePrefix;

    /**
     * Dedicated scheduler dùng cho STOMP heartbeats.
     * Tách riêng khỏi default Spring scheduler để không ảnh hưởng
     * {@code @Scheduled} jobs trong flashsale module.
     */
    @Bean
    public TaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix(threadNamePrefix);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }
}