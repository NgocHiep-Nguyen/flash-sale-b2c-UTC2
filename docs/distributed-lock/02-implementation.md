# 02. Hiện thực Chi Tiết Khóa Phân Tán (Redisson Implementation)

**Ngày cập nhật**: 05/11/2026  
**Dự án**: Flash Sale B2C UTC2  

---

## 1. Danh Sách Class & Method Liên Quan

| Tên Class | Phương thức chính | Đường dẫn file | Vai trò |
| :--- | :--- | :--- | :--- |
| `RedissonConfig` | `redissonClient()` | `src/main/java/com/b2c/flash_sale_b2c_UTC2/config/RedissonConfig.java:30-40` | Khởi tạo Bean `RedissonClient` khi cấu hình `redisson.enabled=true`. |
| `FlashSaleReservationFacade` | `reserve(...)` | `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationFacade.java:63-140` | Điểm tiếp nhận request, kiểm tra fail-fast, xin lock, gọi worker và giải phóng lock trong `finally`. |
| `RedissonLockStockReservationStrategy` | `reserveStock(...)`, `compensate(...)` | `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/strategy/RedissonLockStockReservationStrategy.java:31-89` | Trừ tồn kho và tăng limit trên Redis một cách an toàn khi đã có Lock. Bù trừ kho nếu lỗi. |
| `FlashSaleReservationService` | `createReservation(...)`, `createReservationOptimizedRedisson(...)` | `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationService.java:92-185` | Xử lý kiểm tra idempotency, validate nghiệp vụ, điều phối cập nhật DB. |
| `GatedReservationTransactionalWorker` | `executeReservationInTransaction(...)` | `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/GatedReservationTransactionalWorker.java:43-138` | Giao dịch DB ngắn hạn độc lập (`Propagation.REQUIRES_NEW`), commit xong mới dọn dẹp cache. |
| `LockAcquisitionException` | Constructor | `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/exception/LockAcquisitionException.java:10-18` | Ngoại lệ quăng ra khi hết thời gian `waitMs` mà không xin được khóa. |

---

## 2. Thiết Kế & Tham Số Của Lock

Trong `FlashSaleReservationFacade.java`, việc xin khóa được hiện thực như sau:
```java
String lockKey = LOCK_PREFIX + itemId; // lock:flash_sale:item:{itemId}
RLock lock = redissonClient.getLock(lockKey);
boolean acquired = lock.tryLock(waitMs, TimeUnit.MILLISECONDS);
```

### Chi tiết các tham số thực tế trong code:
1. **Lock Key Pattern**:
   - Định dạng: `lock:flash_sale:item:{itemId}`
   - Dẫn nguồn: `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationFacade.java:37,99`
   - *Lý do*: Granularity ở mức từng sản phẩm Flash Sale (`itemId`). Các sản phẩm khác nhau không tranh chấp lock của nhau, tối đa hóa thông lượng đồng thời giữa nhiều mặt hàng.
2. **Loại Lock (Lock Type)**:
   - Dùng `redissonClient.getLock(...)` tương ứng với Non-fair ReentrantLock mặc định của Redisson.
   - *Code không dùng Fair Lock* (không gọi `getFairLock(...)`).
3. **Thời gian chờ xin lock (`waitTime` / `waitMs`)**:
   - Giá trị cấu hình: Mặc định `200ms` (`@Value("${flashsale.lock.wait-ms:200}")`).
   - Dẫn nguồn: `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationFacade.java:47,105`
   - *Lý do*: Trong kịch bản Flash Sale với hàng ngàn request cùng lúc vào một sản phẩm, việc để request chờ quá lâu (ví dụ vài giây) sẽ làm phình to số luồng chờ (thread exhaustion), tràn connection pool và tăng độ trễ cho người dùng. 200ms là ngưỡng đủ để các luồng xếp hàng kế cận kịp xử lý xong trong khi nhanh chóng giải phóng tài nguyên cho các luồng không có cơ hội.
4. **Thời gian giữ lock tối đa (`leaseTime`) & Redisson Watchdog**:
   - Trong code gọi: `lock.tryLock(waitMs, TimeUnit.MILLISECONDS)` (không truyền tham số `leaseTime`).
   - Theo cơ chế mặc định của Redisson: Khi không truyền `leaseTime`, Redisson sẽ kích hoạt cơ chế **Watchdog** (mặc định lock watchdog timeout là 30.000ms - 30 giây) và tự động gia hạn lock định kỳ mỗi 10 giây (1/3 thời gian watchdog) chừng nào luồng đang nắm giữ lock chưa gọi `unlock()`.
   - *Lý do*: Tránh tình trạng lock bị giải phóng giữa chừng khi DB transaction hoặc mạng nội bộ bị chậm, đảm bảo tính toàn vẹn dữ liệu cho đến khi luồng gọi `unlock()`.

---

## 3. Thứ Tự Giữa Lock Và Transaction (Lock-Transaction Ordering)

> **Nguyên tắc cốt tử**: **LOCK PHẢI BAO NGOÀI TRANSACTION.** Tuyệt đối không đặt Transaction bao ngoài Lock.

- **Vị trí hiện thực**:
  - `FlashSaleReservationFacade.reserve(...)` là method thông thường, **không** có annotation `@Transactional`.
  - Tại đây, lock được xin trước (`lock.tryLock(...)`).
  - Sau khi xin lock thành công, code mới gọi xuống `reservationService.createReservation(...)` hoặc `worker.executeReservationInTransaction(...)` (nơi có `@Transactional` hoặc `@Transactional(propagation = Propagation.REQUIRES_NEW)`).
  - Khi transaction commit xong xuôi và luồng quay trở lại `Facade`, khối `finally` mới tiến hành `lock.unlock()`.
  - Dẫn nguồn: `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationFacade.java:105-138`
- **Lý do kỹ thuật**:
  - Nếu transaction mở trước khi xin lock: Connection DB bị chiếm giữ vô ích trong suốt thời gian chờ lock (`waitMs`), gây cạn kiệt Connection Pool.
  - Nếu transaction commit sau khi nhả lock: Một thread khác có thể lấy được lock ngay khi thread trước nhả, nhưng thread trước vẫn chưa kịp flush/commit xuống database. Thread mới sẽ đọc dữ liệu cũ chưa commit (Dirty Read / Stale Read), dẫn đến race condition và over-selling.

---

## 4. Xử Lý Các Tình Huống Ngoại Lệ & Biên Giới

### 4.1. Không lấy được lock (Lock Acquisition Timeout)
- Khi `lock.tryLock(waitMs)` trả về `false`:
  - Log cảnh báo: `log.warn("Could not acquire Redisson lock for itemId: {} within {}ms", itemId, waitMs);`
  - Ném `LockAcquisitionException`: được ánh xạ trả về mã lỗi HTTP 429 hoặc báo bận cho client.
  - Dẫn nguồn: `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationFacade.java:106-109`

### 4.2. Luồng bị ngắt (InterruptedException)
- Nếu thread bị interrupt khi đang chờ lock:
  - Khôi phục cờ ngắt: `Thread.currentThread().interrupt();`
  - Ném `LockAcquisitionException("Thread interrupted while waiting for lock", e);`
  - Dẫn nguồn: `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationFacade.java:124-126`

### 4.3. Đảm bảo luôn nhả lock (`finally` block)
- Luôn kiểm tra hai điều kiện trước khi nhả:
  ```java
  finally {
      if (acquired && lock.isHeldByCurrentThread()) {
          try {
              lock.unlock();
          } catch (Exception unlockEx) {
              log.error("Failed to unlock Redisson lock for key: {}", lockKey, unlockEx);
          }
      }
  }
  ```
  - Kiểm tra `isHeldByCurrentThread()` ngăn việc thread này cố nhả lock của thread khác nếu lock đã bị chuyển giao hoặc đã hết hạn bất thường.
  - Khối `try-catch` bọc quanh `lock.unlock()` ngăn chặn ngoại lệ lúc nhả lock làm che lấp lỗi nghiệp vụ chính.
  - Dẫn nguồn: `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationFacade.java:127-138`

### 4.4. Lỗi nghiệp vụ giữa chừng & Bù trừ tồn kho (Compensation)
- Nếu kiểm tra thất bại sau khi đã trừ kho Redis (ví dụ: DB sập hoặc lỗi logic):
  - Phương thức `compensate(...)` trong `RedissonLockStockReservationStrategy` sẽ hoàn trả tồn kho Redis bằng `opsForValue().increment(stockKey, quantity)` và giảm `userLimitKey`.
  - Dẫn nguồn: `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/strategy/RedissonLockStockReservationStrategy.java:71-89`

### 4.5. Redis gặp sự cố hoặc mất kết nối
- Nếu `redissonClient == null`: Facade ghi log cảnh báo và tiếp tục chuyển trực tiếp vào service nghiệp vụ (Degradation mode).
  - Dẫn nguồn: `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationFacade.java:94-97`
- Nếu Redis bị ngắt kết nối hoàn toàn khi đang gọi `tryLock()`: Redisson sẽ ném `RedisException` / `RedisConnectionException`. Exception này thoát ra tầng Controller và được chuyển hóa thành mã lỗi hệ thống HTTP 500/503 bởi `GlobalExceptionHandler`.

---

## 5. Cơ Chế Idempotency (Chống Trùng Lặp Yêu Cầu)

Cơ chế Idempotency được tích hợp song song nhằm ngăn chặn việc mạng bị chập chờn hoặc người dùng ấn nút mua nhiều lần với cùng một `Idempotency-Key`:

1. **Khóa Idempotency trên Redis**:
   - Định dạng key: `idempotency:reservation:{userId}:{idempotencyKey}`
   - Dẫn nguồn: `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationService.java:27,96`
2. **TTL (Time-To-Live)**:
   - Được đặt là **10 phút** (`Duration.ofMinutes(10)`).
   - Dẫn nguồn: `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationService.java:97`
3. **Quy trình xử lý**:
   - **Bước 1 (Ghi nhận trạng thái đang xử lý)**: Sử dụng lệnh nguyên tử `setIfAbsent(idemKey, "PROCESSING", 10m)`.
   - **Bước 2 (Kiểm tra request trùng)**:
     - Nếu key đã tồn tại và có giá trị `"PROCESSING"`: Ném `BusinessException(FlashSaleErrorCode.IDEMPOTENCY_CONFLICT)` (HTTP 409) báo cho client yêu cầu đang được xử lý, không gửi lặp lại.
     - Nếu key đã có giá trị là `{orderCode}` (đã xử lý xong trước đó): Hệ thống **không tạo đơn mới**, trả ngay về thông tin đơn hàng cũ kèm thông điệp `"Yêu cầu đã được xử lý thành công trước đó (Idempotent replay)."` (HTTP 200/201).
   - **Bước 3 (Cập nhật kết quả sau commit)**: Khi DB commit thành công, giá trị của key được cập nhật từ `"PROCESSING"` thành mã đơn hàng `{orderCode}` với TTL 10 phút.
   - Dẫn nguồn: `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationService.java:97-113` và `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/GatedReservationTransactionalWorker.java:108-112`

---

## 6. Nguồn Tham Chiếu
- `src/main/java/com/b2c/flash_sale_b2c_UTC2/config/RedissonConfig.java:1-41`
- `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationFacade.java:1-140`
- `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/strategy/RedissonLockStockReservationStrategy.java:1-124`
- `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/GatedReservationTransactionalWorker.java:43-138`
- `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/service/FlashSaleReservationService.java:92-185`
- `src/main/java/com/b2c/flash_sale_b2c_UTC2/flashsale/exception/LockAcquisitionException.java:1-18`
