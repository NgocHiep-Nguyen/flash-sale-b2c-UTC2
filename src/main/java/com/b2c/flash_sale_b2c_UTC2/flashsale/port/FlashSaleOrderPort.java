package com.b2c.flash_sale_b2c_UTC2.flashsale.port;

import java.time.Instant;
import java.util.List;

/**
 * Interface kết nối (Port) giữa Module Flash Sale và Module Order.
 * Phục vụ tạo đơn giữ chỗ PENDING_PAYMENT và xử lý hoàn kho khi đơn hết hạn thanh toán.
 */
public interface FlashSaleOrderPort {

    /**
     * Tạo đơn PENDING_PAYMENT (expires_at = now + ttl) kèm order_items snapshot;
     * chạy trong transaction riêng của bên Order.
     */
    OrderRef createPendingOrder(CreateFlashSaleOrderCommand cmd);

    /**
     * Lấy theo lô các đơn flash sale PENDING_PAYMENT đã quá hạn,
     * khóa SELECT ... FOR UPDATE SKIP LOCKED.
     */
    List<ExpiredOrderRef> lockExpiredPendingOrders(Instant now, int batchSize);

    /**
     * Chuyển PENDING_PAYMENT -> CANCELLED_TIMEOUT bằng UPDATE có điều kiện;
     * trả true nếu thực sự chuyển thành công.
     */
    boolean cancelTimeoutIfPending(Long orderId);

    /**
     * Số đơn PENDING_PAYMENT còn lại thuộc slot (dùng cho job hoàn hàng ế).
     */
    long countPendingBySlot(Long slotId);
}
