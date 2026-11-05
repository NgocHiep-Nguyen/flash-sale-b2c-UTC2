# 03. Kết Quả Kiểm Thử Tải (Load Testing)

**Ngày cập nhật**: 05/11/2026  
**Dự án**: Flash Sale B2C UTC2  

---

## 1. Mục Tiêu & Kịch Bản Kiểm Thử

- **Mục tiêu**: Bắn tải với tốc độ cao (**1.000 req/s** và các mức kiểm soát 600 req/s) vào đồng thời **1 sản phẩm Flash Sale duy nhất** (`itemId = 1`) có số lượng tồn kho khan hiếm (100 và 1.000 sản phẩm).
- **Yêu cầu đối soát**:
  - Không bao giờ xảy ra Over-selling (Số lượng đơn hàng lưu thành công trong Database phải bằng đúng số lượng tồn kho ban đầu).
  - Tồn kho trên Redis và Database sau kiểm thử phải bằng `0` và khớp nhau tuyệt đối.
  - Lỗi `5xx` từ backend phải bằng `0`.

---

## 2. Công Cụ & Hướng Dẫn Chạy Kiểm Thử

### 2.1. Danh sách công cụ & Script trong repo
- **k6 (Grafana k6)**: Công cụ sinh tải HTTP song song.
- **Python 3**: Chạy script điều phối ma trận tự động và tổng hợp thống kê.
- **PostgreSQL Client (`psql`) & Redis CLI (`redis-cli`)**: Dùng trong script đối soát.

| Script / File | Đường dẫn | Mục đích |
| :--- | :--- | :--- |
| `purchase.js` | `loadtest/purchase.js` | Kịch bản k6 executor `constant-arrival-rate`, rate: 1000 req/s, duration: 10s, VUs: 500-2000. |
| `tokens.json` | `loadtest/tokens.json` | Danh sách 1.000 JWT access token và `addressId` của 1.000 user thật được tạo sẵn. |
| `runner.py` | `loadtest/runner.py` | Bộ thư viện thực thi k6, parse output metrics, và truy vấn DB/Redis đối soát. |
| `run_matrix_2x2.py` | `loadtest/run_matrix_2x2.py` | Script chạy tự động ma trận 2x2 (4 cells, 1 warmup + 5 lần đo chính thức cho mỗi mức stock). |
| `measure_lock_detailed.py` | `loadtest/measure_lock_detailed.py` | Script phân tích log timing nano-giây: `hold_time`, `gap_time`, `unlock_time`. |
| `test_idempotency_4cells.py` | `loadtest/test_idempotency_4cells.py` | Kịch bản kiểm tra tính Idempotent khi gửi lặp lại request cùng key hoặc song song. |
| `reset.ps1` / `reset.sh` | `loadtest/reset.ps1` | Script reset dữ liệu DB & Redis cho sản phẩm test về mức stock chỉ định. |
| `verify.ps1` / `verify.sh` | `loadtest/verify.ps1` | Script kiểm tra đơn hàng thực tế trong DB và số tồn kho còn lại. |

### 2.2. Lệnh chạy tái hiện (Reproduction Command)
```powershell
# 1. Khởi động Backend với profile loadtest (Redisson Lock)
.\gradlew.bat bootJar
java -Dspring.profiles.active=loadtest -jar build/libs/flash-sale-b2c-UTC2-0.0.1-SNAPSHOT.jar

# 2. Reset tồn kho sản phẩm ID=1 về mức 100
.\loadtest\reset.ps1 -Stock 100 -ItemId 1

# 3. Chạy k6 load test 1000 req/s trong 10s (tổng 10.000 requests)
k6 run -e BASE_URL="http://localhost:8080" -e ITEM_ID=1 loadtest/purchase.js

# 4. Đối soát dữ liệu DB và Redis
.\loadtest\verify.ps1 -ItemId 1
```

---

## 3. Môi Trường Kiểm Thử

Dữ liệu môi trường được ghi nhận trực tiếp từ các file thực thi và config:
- **Cấu hình máy chủ**: Máy đơn (Single Host, Windows, CPU đa nhân).
- **Backend Application**: 1 instance Spring Boot (Java 25), HikariCP `maximum-pool-size=50`, `minimum-idle=20`.
- **Hệ quản trị CSDL**: PostgreSQL 18 Local (Port 5432) qua đường dẫn `C:\Program Files\PostgreSQL\18\bin\psql.exe`.
- **In-Memory Cache & Lock Store**: Redis Single Node (Port 6379) qua `C:\Program Files\Redis\redis-cli.exe`.

---

## 4. Kết Quả Đo Lường Thực Tế: Ma Trận Kiểm Soát 2x2

Hệ thống đã thực hiện kiểm thử ma trận 2x2 độc lập, kiểm soát hai trục biến thiên:
- **Trục 1 (Concurrency Mechanism)**: `Lua Script` vs `Redisson Distributed Lock`.
- **Trục 2 (DB Transaction Flow)**: `Legacy Flow` (DB transaction dài bao trùm) vs `Optimized Flow` (`REQUIRES_NEW` ngắn hạn).

Tất cả các cell đều được đo lường chính thức 5 lần độc lập (sau 1 lần warm-up đã bị loại bỏ) với kịch bản **10.000 requests trong 10 giây (1.000 req/s)**. Dưới đây là bảng số liệu trung bình và dải biến thiên thực tế được trích xuất từ file kết quả `loadtest/results/summary_table.json`:

| Kịch bản / Ô Thử Nghiệm (Cell) | HTTP 200 (Thành công) | HTTP 409 (Hết hàng) | HTTP 429 (Hết hạn Lock) | HTTP 5xx (Lỗi Server) | DB Orders Tạo ra | Tồn kho DB/Redis cuối | p95 Latency 200 (ms) | Throughput thực tế (req/s) | Over-selling? |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Cell 1: Lua + Legacy** *(Stock 100)* | 100 | 9.900 *(9.749-9.901)* | 0 | 0 | 100 | 0 / 0 | 776.97 ms | 117.74 | **KHÔNG** |
| **Cell 1: Lua + Legacy** *(Stock 1.000)* | 1.000 | 7.350 *(6.993-7.413)* | 0 | 0 | 1.000 | 0 / 0 | 3.176.43 ms | 198.27 | **KHÔNG** |
| **Cell 2: Lua + Optimized** *(Stock 100)* | 100 | 9.900 *(9.900-9.901)* | 0 | 0 | 100 | 0 / 0 | 313.76 ms | 339.46 | **KHÔNG** |
| **Cell 2: Lua + Optimized** *(Stock 1.000)* | 1.000 | 6.717 *(4.629-7.400)* | 0 | 0 | 1.000 | 0 / 0 | 3.382.92 ms | 219.39 | **KHÔNG** |
| **Cell 3: Redisson + Legacy** *(Stock 100)* | 100 | 8.701 *(8.148-8.798)* | 1.199 *(1.103-1.753)* | 0 | 100 | 0 / 0 | 217.29 ms | 78.67 | **KHÔNG** |
| **Cell 3: Redisson + Legacy** *(Stock 1.000)* | 912 *(883-928)* | 0 | 9.072 *(9.061-9.112)* | 0 | 912 *(883-928)* | 88 / 88 *(72-117)* | 214.48 ms | 91.42 | **KHÔNG** |
| **Cell 4: Redisson + Optimized** *(Stock 100)* | 100 | 8.223 *(8.098-8.830)* | 1.677 *(1.071-1.803)* | 0 | 100 | 0 / 0 | 224.98 ms | 56.77 | **KHÔNG** |
| **Cell 4: Redisson + Optimized** *(Stock 1.000)* | 917 *(789-984)* | 0 | 9.077 *(9.001-9.200)* | 0 | 917 *(789-984)* | 83 / 83 *(16-211)* | 214.36 ms | 91.94 | **KHÔNG** |

> **Nhận xét quan trọng về kết quả Redisson Lock**:
> 1. **Tuyệt đối không Over-selling**: Trong cả hai cell Redisson (Cell 3 & Cell 4) với Stock 100, chính xác 100 đơn hàng được tạo, tồn kho DB và Redis về 0.
> 2. **Hiện tượng Lock Timeout khi Stock lớn (Stock = 1.000)**:
>    - Khi số lượng tồn kho là 1.000 nhưng thời gian test chỉ có 10 giây và thời gian giữ lock trung bình khoảng 7-8ms mỗi request, hệ thống chỉ có thể phục vụ tuần tự tối đa ~100-125 đơn hàng thành công mỗi giây.
>    - Do đó, trong 10 giây thử nghiệm, hệ thống chỉ kịp xử lý thành công ~912 - 917 đơn.
>    - Các request còn lại bị hết thời gian chờ `waitMs=200ms` và fail-fast trả về HTTP 429 (`count_429_lock_timeout` chiếm ~9.070 requests), hoàn toàn không có lỗi 5xx. Tồn kho còn lại trong DB và Redis khớp nhau chính xác (còn 83 - 88 sản phẩm, chưa bán hết vì thời gian test kết thúc).

---

## 5. Đo Lường Chi Tiết Thời Gian Giữ Khóa (Redisson Lock Profiling)

Từ kết quả đo lường thời gian nano-giây ở cấp độ JVM (`loadtest/results/redisson_detailed_lock_comparison.json`), các chỉ số profile của Redisson Lock được thống kê chi tiết như sau:

| Chỉ số Profiling | Redisson Legacy (Cell 3) | Redisson Optimized (Cell 4) | Ý nghĩa kỹ thuật |
| :--- | :--- | :--- | :--- |
| **Hold Time (Trung bình)** | **7.07 ms** | **7.72 ms** | Thời gian luồng chiếm giữ lock (từ lúc acquire đến release). |
| **Hold Time (Median / p50)** | **6.89 ms** | **7.11 ms** | 50% số request xử lý xong và nhả lock trong khoảng ~7ms. |
| **Hold Time (p95)** | **9.34 ms** | **10.95 ms** | 95% request giữ lock dưới 11ms. |
| **Gap Time (Median / p50)** | **0.84 ms** | **0.88 ms** | Khoảng trống chuyển giao lock giữa luồng trước và luồng sau trên Redis. |
| **Unlock Time (Trung bình)** | **0.35 ms** | **0.35 ms** | Thời gian round-trip mạng để Redisson thực thi lệnh unlock lên Redis. |

---

## 6. Nguồn Tham Chiếu
- `loadtest/purchase.js:1-119`
- `loadtest/runner.py:1-130`
- `loadtest/run_matrix_2x2.py:1-78`
- `loadtest/results/summary_table.json:106-210`
- `loadtest/results/redisson_detailed_lock_comparison.json:1-182`
- `loadtest/results/idempotency_table_4cells.json:56-110`
