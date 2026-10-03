package com.b2c.flash_sale_b2c_UTC2.flashsale.strategy;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("redissonLockStockReservationStrategy")
@ConditionalOnProperty(name = "flashsale.reservation.strategy", havingValue = "redisson")
public class RedissonLockStockReservationStrategy implements StockReservationStrategy {

    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;

    public static final String LOCK_PREFIX = "lock:flash_sale:item:";
    public static final String STOCK_KEY_PREFIX = "flash_sale:stock:";
    public static final String USER_LIMIT_KEY_PREFIX = "flash_sale:user_limit:";

    @Autowired
    public RedissonLockStockReservationStrategy(
            @Autowired(required = false) RedissonClient redissonClient,
            @Autowired(required = false) StringRedisTemplate redisTemplate
    ) {
        this.redissonClient = redissonClient;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long reserveStock(Long slotId, Long userId, Long itemId, int quantity, int userPurchaseLimit, long slotRemainingTtlSeconds) {
        if (redissonClient == null || redisTemplate == null) {
            log.warn("Redisson or Redis not available. Reservation skipped.");
            return 1;
        }

        String lockKey = LOCK_PREFIX + itemId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Could not acquire lock for FlashSaleItem ID: {}", itemId);
                return 0;
            }

            try {
                String stockKey = STOCK_KEY_PREFIX + itemId;
                String userLimitKey = USER_LIMIT_KEY_PREFIX + slotId + ":" + userId + ":" + itemId;

                String userCountStr = redisTemplate.opsForValue().get(userLimitKey);
                int userPurchased = userCountStr != null ? Integer.parseInt(userCountStr) : 0;
                if (userPurchased + quantity > userPurchaseLimit) {
                    return -1;
                }

                String stockStr = redisTemplate.opsForValue().get(stockKey);
                int currentStock = stockStr != null ? Integer.parseInt(stockStr) : 0;
                if (currentStock < quantity) {
                    return 0;
                }

                redisTemplate.opsForValue().decrement(stockKey, quantity);
                redisTemplate.opsForValue().increment(userLimitKey, quantity);
                if (slotRemainingTtlSeconds > 0) {
                    redisTemplate.expire(userLimitKey, Duration.ofSeconds(slotRemainingTtlSeconds));
                }

                return 1;
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting for Redisson lock", e);
        }
    }

    @Override
    public void compensate(Long slotId, Long userId, Long itemId, int quantity) {
        if (redisTemplate == null) {
            return;
        }

        String stockKey = STOCK_KEY_PREFIX + itemId;
        String userLimitKey = USER_LIMIT_KEY_PREFIX + slotId + ":" + userId + ":" + itemId;

        try {
            redisTemplate.opsForValue().increment(stockKey, quantity);
            Long remainingLimit = redisTemplate.opsForValue().decrement(userLimitKey, quantity);
            if (remainingLimit != null && remainingLimit <= 0) {
                redisTemplate.delete(userLimitKey);
            }
            log.info("Compensated Redisson reservation stock (+{}) and limit for FlashSaleItem ID: {}, user ID: {}", quantity, itemId, userId);
        } catch (Exception e) {
            log.error("Failed to compensate in Redisson strategy for item ID: {}, user ID: {}: {}", itemId, userId, e.getMessage(), e);
        }
    }
}
