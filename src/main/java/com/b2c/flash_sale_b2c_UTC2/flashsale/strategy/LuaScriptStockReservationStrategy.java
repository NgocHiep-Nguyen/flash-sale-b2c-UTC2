package com.b2c.flash_sale_b2c_UTC2.flashsale.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("luaScriptStockReservationStrategy")
@ConditionalOnProperty(name = "flashsale.reservation.strategy", havingValue = "lua", matchIfMissing = true)
public class LuaScriptStockReservationStrategy implements StockReservationStrategy {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> reserveScript;

    public static final String STOCK_KEY_PREFIX = "flash_sale:stock:";
    public static final String USER_LIMIT_KEY_PREFIX = "flash_sale:user_limit:";

    @Autowired
    public LuaScriptStockReservationStrategy(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.reserveScript = new DefaultRedisScript<>();
        this.reserveScript.setLocation(new ClassPathResource("lua/reserve_stock.lua"));
        this.reserveScript.setResultType(Long.class);
    }

    @Override
    public long reserveStock(Long slotId, Long userId, Long itemId, int quantity, int userPurchaseLimit, long slotRemainingTtlSeconds) {
        if (redisTemplate == null) {
            log.warn("Redis is not available. Lua reservation skipped.");
            return 1;
        }

        String stockKey = STOCK_KEY_PREFIX + itemId;
        String userLimitKey = USER_LIMIT_KEY_PREFIX + slotId + ":" + userId + ":" + itemId;

        try {
            Long result = redisTemplate.execute(
                    reserveScript,
                    List.of(stockKey, userLimitKey),
                    String.valueOf(quantity),
                    String.valueOf(userPurchaseLimit),
                    String.valueOf(Math.max(slotRemainingTtlSeconds, 60))
            );

            if (result == null) {
                return 0;
            }
            if (result == -1) {
                return -1; // user limit exceeded
            }
            if (result == 0 && !hasStockInRedis(stockKey)) {
                return 0; // out of stock
            }
            return 1; // success
        } catch (Exception e) {
            log.error("Error executing reserve_stock.lua for item ID: {}, user ID: {}: {}", itemId, userId, e.getMessage());
            throw e;
        }
    }

    private boolean hasStockInRedis(String stockKey) {
        try {
            String val = redisTemplate.opsForValue().get(stockKey);
            return val != null && Integer.parseInt(val) >= 0;
        } catch (Exception e) {
            return false;
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
            log.info("Compensated Redis stock (+{}) and limit for FlashSaleItem ID: {}, user ID: {}", quantity, itemId, userId);
        } catch (Exception e) {
            log.error("Failed to compensate Redis for item ID: {}, user ID: {}: {}", itemId, userId, e.getMessage(), e);
        }
    }
}
