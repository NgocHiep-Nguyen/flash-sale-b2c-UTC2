# Flash Sale - Order Module Integration Contract

## 1. Giới thiệu & Mục đích
Tài liệu này định nghĩa giao diện (Contract / Port) giữa **Module Flash Sale** và **Module Order** trong dự án `flash-sale-b2c-UTC2`.
Module Flash Sale chịu trách nhiệm kiểm soát tồn kho phân bổ trên Redis (chống over-selling, atomicity bằng Lua script) và giữ chỗ.
Module Order chịu trách nhiệm tạo và quản lý vòng đời đơn hàng trong PostgreSQL (`orders`, `order_items`).

---

## 2. Giao diện `FlashSaleOrderPort`

Package: `com.b2c.flash_sale_b2c_UTC2.flashsale.port`

```java
public interface FlashSaleOrderPort {

    /**
     * Tạo đơn hàng ở trạng thái PENDING_PAYMENT (expires_at = now + ttl) kèm order_items snapshot.
     * Chạy trong transaction riêng của module Order.
     * 
     * @param cmd Dữ liệu snapshot để tạo đơn
     * @return Tham chiếu đơn hàng (orderId, orderCode)
     */
    OrderRef createPendingOrder(CreateFlashSaleOrderCommand cmd);

    /**
     * Lấy theo lô danh sách đơn hàng Flash Sale ở trạng thái PENDING_PAYMENT đã quá hạn thanh toán.
     * Cần sử dụng khóa SELECT ... FOR UPDATE SKIP LOCKED để tránh tranh chấp giữa nhiều worker.
     * 
     * @param now Thời điểm kiểm tra hiện tại
     * @param batchSize Số lượng đơn tối đa quét trong 1 đợt
     * @return Danh sách các đơn đã hết hạn cần hoàn kho
     */
    List<ExpiredOrderRef> lockExpiredPendingOrders(Instant now, int batchSize);

    /**
     * Chuyển trạng thái đơn hàng PENDING_PAYMENT -> CANCELLED_TIMEOUT bằng câu lệnh UPDATE có điều kiện.
     * 
     * @param orderId ID đơn hàng cần hủy do hết hạn
     * @return true nếu trạng thái đơn thực sự được chuyển từ PENDING_PAYMENT sang CANCELLED_TIMEOUT; 
     *         false nếu đơn đã được thanh toán hoặc đã hủy trước đó.
     */
    boolean cancelTimeoutIfPending(Long orderId);

    /**
     * Đếm số lượng đơn hàng PENDING_PAYMENT còn tồn tại thuộc một khung giờ (slot).
     * Dùng cho job tự động đóng slot và hoàn tồn kho chưa bán về kho gốc của người bán.
     * 
     * @param slotId ID khung giờ Flash Sale
     * @return Số lượng đơn hàng đang chờ thanh toán
     */
    long countPendingBySlot(Long slotId);
}
```

---

## 3. Data Transfer Objects (Records)

### 3.1 `CreateFlashSaleOrderCommand`
Dữ liệu gửi từ Flash Sale sang Order để tạo đơn giữ chỗ:
- `Long userId`: ID người mua hàng.
- `Long addressId`: ID địa chỉ nhận hàng (đã được xác thực quyền sở hữu).
- `Long flashSaleItemId`: ID bản ghi flash_sale_items.
- `Long variantId`: ID biến thể phân loại (SKU) sản phẩm.
- `Long storeId`: ID gian hàng người bán.
- `Integer quantity`: Số lượng sản phẩm mua (thường là 1 hoặc tuân thủ user purchase limit).
- `BigDecimal unitPrice`: Đơn giá Flash Sale tại thời điểm mua (`flash_sale_price`).
- `String productName`: Snapshot tên sản phẩm SPU.
- `String variantName`: Snapshot tên phân loại biến thể SKU.
- `String orderCode`: Mã đơn hàng được sinh tự động (UUID / chuỗi duy nhất).
- `Instant expiresAt`: Thời điểm hết hạn giữ chỗ (ví dụ: `now + 300s`).

### 3.2 `OrderRef`
- `Long orderId`: Khóa chính `orders.id`.
- `String orderCode`: Mã hiển thị đơn hàng `orders.order_code`.

### 3.3 `ExpiredOrderRef`
- `Long orderId`: ID đơn hàng.
- `Long flashSaleItemId`: ID mặt hàng flash sale để hoàn tồn kho Redis & DB.
- `Long slotId`: ID khung giờ Flash Sale.
- `Long userId`: ID người mua để hoàn hạn mức mua (`user_limit`).
- `Integer quantity`: Số lượng sản phẩm cần hoàn trả.

---

## 4. Quy ước & Phối hợp Nghiệp vụ

1. **Transaction Boundary**:
   - `createPendingOrder`: Thực thi trong transaction riêng của Order. Nếu ném ngoại lệ (`RuntimeException`), Flash Sale sẽ tự động bắt ngoại lệ và **bù hoàn Redis ngay lập tức** (tăng lại tồn kho Redis và giảm bộ đếm `user_limit`).
2. **Idempotency khi Hủy Timeout**:
   - `cancelTimeoutIfPending` **phải** là câu lệnh nguyên tử:
     ```sql
     UPDATE orders 
     SET status = 'CANCELLED_TIMEOUT', updated_at = NOW() 
     WHERE id = :orderId AND status = 'PENDING_PAYMENT';
     ```
   - Chỉ khi kết quả trả về `1 row updated` (method trả về `true`), Flash Sale mới thực hiện hoàn kho Redis và cộng lại `available_stock` trên PostgreSQL. Điều này ngăn chặn hoàn kho 2 lần khi nhiều worker chạy song song.
3. **Thông báo Chốt Kho khi Thanh toán thành công (PAID)**:
   - Khi thanh toán thành công, đơn hàng chuyển từ `PENDING_PAYMENT` sang `PROCESSING` / `PAID`.
   - Lúc này kho Flash Sale không cần hoàn lại. Nếu cần đồng bộ trạng thái, bên Order có thể phát sự kiện `OrderPaidEvent(orderId, flashSaleItemId)`.
