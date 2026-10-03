package com.b2c.flash_sale_b2c_UTC2.flashsale.port;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Command chứa dữ liệu snapshot cần thiết để module Order tạo đơn PENDING_PAYMENT.
 */
public record CreateFlashSaleOrderCommand(
        Long userId,
        Long addressId,
        Long flashSaleItemId,
        Long slotId,
        Long variantId,
        Long storeId,
        Integer quantity,
        BigDecimal unitPrice,
        String productName,
        String variantName,
        String orderCode,
        Instant expiresAt
) {
}
