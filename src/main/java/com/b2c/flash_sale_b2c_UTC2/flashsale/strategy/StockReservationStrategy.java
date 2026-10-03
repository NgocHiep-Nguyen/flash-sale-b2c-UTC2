package com.b2c.flash_sale_b2c_UTC2.flashsale.strategy;

public interface StockReservationStrategy {
    /**
     * Attempts atomic stock reservation and user limit increment.
     *
     * @param slotId flash sale slot ID
     * @param userId user ID
     * @param itemId flash sale item ID
     * @param quantity quantity requested
     * @param userPurchaseLimit max allowed per user for this slot
     * @param slotRemainingTtlSeconds remaining duration of the slot
     * @return 1 if success, -1 if limit exceeded, 0 if out of stock
     */
    long reserveStock(Long slotId, Long userId, Long itemId, int quantity, int userPurchaseLimit, long slotRemainingTtlSeconds);

    /**
     * Compensates (rolls back) the reservation in cache if downstream actions fail.
     */
    void compensate(Long slotId, Long userId, Long itemId, int quantity);

    /**
     * Safe rollback of stock that prevents orphan INCRBY on non-existent keys.
     */
    void rollbackStockSafe(Long slotId, Long userId, Long itemId, int quantity, int fallbackStock, long remainingTtlSeconds);
}
