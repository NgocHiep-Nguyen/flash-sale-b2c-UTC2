package com.b2c.flash_sale_b2c_UTC2.flashsale.port;

/**
 * Tham chiếu đơn hàng sau khi tạo thành công.
 */
public record OrderRef(
        Long orderId,
        String orderCode
) {
}
