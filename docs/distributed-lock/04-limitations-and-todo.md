# 04. Giới Hạn Đã Biết & Kế Hoạch Cải Tiến (Limitations & TODO)

**Ngày cập nhật**: 05/11/2026  
**Dự án**: Flash Sale B2C UTC2  

---

## 1. Các Giới Hạn & Rủi Ro Đã Xác Minh (Verified)

Những nội dung dưới đây đã được chứng minh và ghi nhận trực tiếp qua code, cấu hình và kết quả thực nghiệm kiểm thử tải:

### 1.1. Hiện tượng thắt cổ chai do tuần tự hóa đơn điểm (Single Key Serialization Bottleneck)
- **Đã xác minh**: Khi nhiều luồng cùng tranh chấp một khóa `lock:flash_sale:item:{itemId}`, các request phải xếp hàng tuần tự.
- **Bằng chứng số liệu**:
  - Thời gian giữ khóa trung bình là `~7.07ms - 7.72ms` mỗi request (`loadtest/results/redisson_detailed_lock_comparison.json:68,158`).
  - Giới hạn vật lý của thông lượng trên 1 mặt hàng chỉ đạt tối đa khoảng `100 - 130 requests/giây`.
  - Khi tồn kho là 1.000 sản phẩm trong khung thử nghiệm 10 giây (yêu cầu thông lượng tối thiểu 100 req/s), khoảng `~90%` request bị quá hạn `waitMs=200ms` và phải nhận mã lỗi HTTP 429 (`loadtest/results/summary_table.json:126,178`).

### 1.2. Phụ thuộc vào Redis Single Node
- **Đã xác minh**: Cấu hình `RedissonConfig` đang sử dụng mô hình kết nối `useSingleServer()` (`src/main/java/com/b2c/flash_sale_b2c_UTC2/config/RedissonConfig.java:35`).
- **Hệ quả**: Nếu Redis node này gặp sự cố (crash, restart, network partition), toàn bộ cơ chế khóa phân tán sẽ bị gián đoạn.

### 1.3. Hành vi Idempotency với Concurrent Request cùng Key trong Redisson Lock
- **Đã xác minh qua test**: Trong kịch bản test `test_idempotency_4cells.py` (`loadtest/results/idempotency_table_4cells.json:73-81`), khi 2 request có cùng một `Idempotency-Key` được bắn đồng thời trong chế độ Redisson Lock:
  - Do Redisson Lock tuần tự hóa 2 request này hoàn hảo, request thứ nhất vào xử lý thành công và lưu kết quả `{orderCode}` vào cache.
  - Request thứ hai vào sau khi lock được nhả, đọc trúng giá trị kết quả đã cache và trả về HTTP 201 replay thành công mà **không** tạo thêm đơn mới trong DB (số đơn DB tạo ra chính xác là 1).
  - Không phát sinh lỗi xung đột 409 như ở phía Lua script.

---

## 2. Các Rủi Ro & Hạng Mục Chưa Xác Minh / Chưa Làm

Các hạng mục dưới đây chưa được kiểm chứng trong môi trường thực tế hoặc chưa triển khai trong codebase:

### 2.1. Chưa kiểm thử trên cụm Redis Cluster / Sentinel
- **Trạng thái**: **Chưa làm**
- **Lý do**: Cấu hình hiện tại chỉ mới hỗ trợ Redis đơn (`useSingleServer`). Hệ thống chưa cấu hình Redlock Algorithm hoặc kiểm thử failover trên cụm Redis Cluster/Sentinel khi node Master bị ngắt đột ngột.

### 2.2. Chưa kiểm thử với nhiều instance Backend phân tán (Multi-instance Load Balancing)
- **Trạng thái**: **Chưa xác minh**
- **Lý do**: Các đợt kiểm thử k6 đều được chạy nhắm vào 1 instance Spring Boot duy nhất (`http://localhost:8080`). Mặc dù về lý thuyết Redisson phân tán hoạt động tốt giữa nhiều node thông qua Redis tập trung, hành vi cạnh tranh khóa giữa nhiều server JVM vật lý khác nhau chưa có log thực nghiệm.

### 2.3. Chưa triển khai cơ chế Phân đoạn kho (Stock Sharding / Virtual Buckets)
- **Trạng thái**: **Chưa làm**
- **Lý do**: Hiện tại toàn bộ tồn kho của 1 sản phẩm Flash Sale đều gom vào 1 key duy nhất. Để nâng thông lượng Redisson Lock vượt qua giới hạn ~130 req/s lên hàng ngàn req/s trên 1 SKU, cần áp dụng giải pháp chia nhỏ kho thành nhiều bucket (ví dụ `flash_sale:stock:{itemId}:bucket_1..N`) và khóa phân tán theo từng bucket. Kỹ thuật này hiện chưa được hiện thực trong code.

### 2.4. Khả năng tự động điều chỉnh `waitMs` theo mức tải (Adaptive Wait Time)
- **Trạng thái**: **Chưa làm**
- **Lý do**: Giá trị `flashsale.lock.wait-ms` hiện đang là giá trị tĩnh được cấu hình trong file yaml (`200ms`). Khi hệ thống có độ trễ I/O tăng cao, việc giữ thời gian chờ cố định có thể làm tăng tỉ lệ rớt 429.

---

## 3. Kế Hoạch Cải Tiến Đề Xuất (TODO Roadmap)

1. **Giai đoạn 1**: Nâng cấp `RedissonConfig` để hỗ trợ môi trường Redis Sentinel / Redis Cluster cho môi trường Production (hỗ trợ tự động chuyển đổi master khi có sự cố).
2. **Giai đoạn 2**: Triển khai giải pháp **Lua Script làm First-Line Gatekeeper** kết hợp Fail-Fast: Request chỉ xin Redisson lock nếu Lua script xác nhận còn hàng và người dùng chưa vượt giới hạn, giảm tải `90%` lượt tranh chấp lock vô ích.
3. **Giai đoạn 3**: Thiết kế mô hình **Stock Sharding**: Chia số lượng tồn kho của một sản phẩm thành nhiều phân vùng nhỏ (kho ảo) để cho phép nhiều lock chạy song song, giải quyết triệt để nút thắt cổ chai throughput.

---

## 4. Nguồn Tham Chiếu
- `src/main/java/com/b2c/flash_sale_b2c_UTC2/config/RedissonConfig.java:30-40`
- `loadtest/results/redisson_detailed_lock_comparison.json:65-90,155-180`
- `loadtest/results/summary_table.json:106-210`
- `loadtest/results/idempotency_table_4cells.json:56-110`
