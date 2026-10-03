# BƯỚC 4: BÁO CÁO PHÂN TÍCH KIẾN TRÚC & CẤU TRÚC HỆ THỐNG
## Đề tài: Hệ thống Sàn Flash Sale B2C Chống Over-selling bằng Distributed Lock trên Redis

---

* **Học phần:** Đồ án Kỹ thuật Phần mềm / Hệ thống Phân tán (UTC2)
* **Mô hình:** Sàn Thương mại điện tử B2C Đa người bán (Multi-Vendor Marketplace)
* **Trọng tâm kỹ thuật:** Concurrency Control, Chống Over-selling, Giữ kho có thời hạn (Reservation TTL 300s), Tách đơn Đa gian hàng (Order Splitting), Thanh toán ZaloPay QR & COD.

---

## 1. Mục Đích & Các Phân Hệ Chính

1. **Admin Portal:**
   * Mở & quản lý các khung giờ Flash Sale (`flash_sale_slots`).
   * Phê duyệt sản phẩm Seller đăng ký (trừ trực tiếp `allocated_stock` từ kho gốc `product_variants.stock_quantity`).
   * Cấu hình tỷ lệ hoa hồng linh hoạt (mặc định 5% toàn sàn, override theo shop).
   * Giám sát sổ cái đối soát tài chính (`wallet_transactions`).

2. **Seller Portal:**
   * Quản lý gian hàng (`stores`) và kho lấy hàng (`addresses`).
   * Quản lý sản phẩm gốc (`products` - SPU) và biến thể phân loại (`product_variants` - SKU).
   * Đăng ký biến thể tham gia Flash Sale: giá sale, số lượng mở bán (`allocated_stock`), trần mua trên mỗi khách (`user_purchase_limit`).
   * Quản lý đơn hàng và theo dõi số dư ví doanh thu (`wallets`).

3. **Buyer Storefront:**
   * Đăng ký/đăng nhập qua Local (Email/Password) và OAuth2 (Google, Facebook, GitHub).
   * Sổ địa chỉ chuẩn hóa (`addresses`) kèm tọa độ GPS và cờ `is_default`.
   * Giỏ hàng đa gian hàng (`carts`, `cart_items`) với cơ chế tự động tách đơn (Order Splitting).
   * Đặt mua Flash Sale với cơ chế giữ chỗ 5 phút (Reservation TTL 300s).
   * Thanh toán đa kênh: Quét mã QR ZaloPay & COD.
   * Đánh giá sản phẩm đã mua (`product_reviews`) chuẩn Verified Purchase.

---

## 2. Tech Stack & Dependencies

* **Ngôn ngữ & Runtime:** Java 25 LTS (build 25.0.4+7-LTS-189).
* **Framework:** Spring Boot 4.1.1 (WebMVC, Data JPA, Security, Validation).
* **Build tool:** Gradle 9.7.1.
* **CSDL:** PostgreSQL 16+ (Flyway quản lý migration, Hibernate `ddl-auto: validate`).
* **Cache & Khóa phân tán:** Redis (Lettuce + Lua Script làm mặc định chống over-selling; Redisson để benchmark so sánh sau).
* **Xác thực:** Spring Security + JWT (`io.jsonwebtoken:jjwt`).
* **Tiện ích:** MapStruct 1.6.3 (`componentModel = "spring"`), Lombok.
* **Tài liệu API:** Springdoc OpenAPI Starter WebMVC UI 3.1.0 (`/swagger-ui.html`).

---

## 3. Kiến Trúc 24 Bảng & Invariants Cốt Lõi

Hệ thống tuân thủ 24 bảng dữ liệu chuẩn 3NF:
1. `roles`
2. `users`
3. `permission_groups`
4. `permissions`
5. `group_permissions`
6. `user_roles`
7. `auth_accounts`
8. `stores`
9. `addresses` (XOR CHECK constraint giữa `user_id` và `store_id`)
10. `wallets`
11. `categories`
12. `products` (SPU)
13. `product_variants` (SKU)
14. `flash_sale_slots`
15. `flash_sale_items`
16. `vouchers`
17. `orders` (1 Order thuộc 1 Store)
18. `order_items`
19. `payments` (Partial Unique Index: tối đa 1 SUCCESS payment / order)
20. `wallet_transactions`
21. `voucher_usages`
22. `carts`
23. `cart_items`
24. `product_reviews` (Verified Purchase)

### 8 Ràng Buộc Invariant Flash Sale:
1. $0 \le \texttt{available\_stock} \le \texttt{allocated\_stock}$ (CHECK constraint DB).
2. Khi Admin duyệt Flash Sale, trích trực tiếp `allocated_stock` từ `product_variants.stock_quantity`.
3. Seller không được giảm base stock xuống dưới mức đang allocate cho Flash Sale đang active.
4. Trừ kho trên Redis bằng Lua Script là nguyên tử, không bao giờ âm kho.
5. Khi đơn hàng timeout (300s) hoặc bị hủy, hoàn trả chính xác số lượng đã đặt: `INCRBY stock {reservedQuantity}` và `DECRBY user_limit {reservedQuantity}`.
6. Xử lý đóng phiên và hoàn hàng ế (`Unsold Stock Rollback`) là Idempotent.
7. Pre-warm Redis trước giờ mở bán là Idempotent, không nhân đôi stock.
8. Bù hoàn hai chiều: Trừ Redis thành công nhưng tạo đơn DB fail phải kích hoạt **Try-Catch Compensation** tức thì trên Redis.

---

## 4. Quy Ước Code & Thực Thi

* **API Format:** Toàn bộ API bọc trong `ApiResponse<T>`, danh sách phân trang bọc trong `PageResponse<T>`.
* **Exception:** Bắt lỗi tập trung qua `GlobalExceptionHandler`, ném lỗi qua `BusinessException(ErrorCode)`.
* **Lộ trình triển khai:**
  1. `auth` + `user` (JWT Token, UserDetailsService, Matrix RBAC).
  2. `product` + `store` (SPU - SKU, Store management).
  3. `flashsale` + `order` (Lettuce + Lua Script, Redis Reservation, Compensation, Rollback).
  4. `cart` (Multi-vendor order splitting).
  5. `payment` (ZaloPay QR Sandbox, COD, Webhook callback).
  6. `wallet` + `voucher` + `review`.
