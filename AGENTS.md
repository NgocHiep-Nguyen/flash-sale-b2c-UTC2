# AGENTS.md

## 1. PROJECT OVERVIEW

### Project
**Flash Sale B2C - UTC2**

Đây là project B2C Multi-Vendor Marketplace kết hợp Flash Sale, phục vụ mục đích đồ án.

### Technology Stack

- Java 25
- Spring Boot
- Gradle
- PostgreSQL
- Spring Data JPA / Hibernate
- Flyway
- Spring Security
- JWT
- Redis
- Redis Lua Script
- MapStruct
- Lombok
- Springdoc OpenAPI / Swagger

### Current Development Phase

Đã hoàn thành:
1. Planning
2. Requirement
3. Design

Đang bước vào:
4. Implementation

---

## 2. SOURCE OF TRUTH

Agent phải đọc và tuân thủ:
1. Code hiện tại.
2. Các tài liệu trong `docs/`.
3. File `AGENTS.md`.
4. Convention và cấu trúc hiện có.

### Quy tắc `docs/`

- `docs/` chứa tài liệu thiết kế đã được chốt.
- **Không tự ý sửa, xóa, đổi tên hoặc format lại file trong `docs/`.**
- Chỉ sửa `docs/` khi user yêu cầu rõ ràng.
- Nếu code khác tài liệu, không tự ý thay đổi thiết kế lớn; báo lại trước.

---

## 3. AGENT WORKFLOW

Mỗi task nên thực hiện:

1. Understand
2. Inspect
3. Read relevant docs
4. Plan
5. Implement
6. Validate
7. Review diff
8. Report

Không nhảy trực tiếp vào code nếu task liên quan architecture, database, concurrency hoặc business flow.

---

## 4. JAVA VERSION - MANDATORY

Project sử dụng **Java 25**.

Máy development có thể mặc định Java 21.



## 5. PROJECT STRUCTURE

Main package:

```text
src/main/java/com/b2c/flash_sale_b2c_UTC2/
```

Module chính:

```text
auth/
user/
store/
product/
flashsale/
cart/
order/
payment/
voucher/
wallet/
review/
```

Common/config:

```text
common/
config/
```

Mỗi module nên tổ chức theo layer khi phù hợp:

```text
controller/
service/
repository/
entity/
dto/
mapper/
```

Không tạo package/module chỉ để làm code phức tạp hơn nếu chưa có nhu cầu.

---

## 6. ARCHITECTURE RULES

Project sử dụng kiến trúc phân tầng.

### Controller
- Nhận HTTP request.
- Validate input.
- Gọi Service.
- Trả HTTP response.
- Không chứa business logic phức tạp.

### Service
- Business logic.
- Transaction boundary.
- Ownership check.
- State validation.
- Điều phối Repository, Redis và component khác.

### Repository
- Database access.
- Query.
- Lock khi thực sự cần.
- Không chứa business logic.

### Entity
- Mapping database.
- Quan hệ JPA.
- Constraint mapping khi phù hợp.
- Không đặt business workflow phức tạp vào Entity.

### DTO
Dùng cho API input/output. Không expose Entity trực tiếp nếu không có lý do rõ ràng.

### Mapper
Sử dụng MapStruct khi phù hợp:

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {
}
```

---

## 7. COMMON API RULES

Nếu project sử dụng:

```text
ApiResponse
PageResponse
```

thì API phải tuân thủ format thống nhất.

Exception xử lý tập trung qua:

```text
BusinessException
GlobalExceptionHandler
ErrorCode
```

Không trả stack trace hoặc exception nội bộ trực tiếp cho client.

---

## 8. DATABASE

Database:

```text
PostgreSQL
```

Migration:

```text
Flyway
```

Migration đầu tiên:

```text
V1__init_schema.sql
```

Thay đổi database sau này phải tạo migration mới:

```text
V2__...
V3__...
```

Không sửa migration đã áp dụng trong môi trường dùng chung nếu không có lý do đặc biệt.

### Hibernate

Dùng:

```yaml
ddl-auto: validate
```

Không dùng `create` hoặc `create-drop` làm cơ chế quản lý schema chính.

Flyway là nguồn quản lý database schema.

---

## 9. DATABASE INTEGRITY

Giữ các constraint đã thiết kế:

- Primary Key
- Foreign Key
- UNIQUE
- NOT NULL
- CHECK
- Index
- Default value

Phân biệt rõ:

### Database Constraint
Đảm bảo data integrity ở database.

### Service Business Rule
Đảm bảo nghiệp vụ.

### Redis Concurrency Rule
Đảm bảo atomicity/concurrency của Flash Sale.

---

## 10. 24-TABLE DESIGN

Giữ nguyên kiến trúc 24 bảng đã thiết kế.

Không tự ý:
- Thêm Parent Order.
- Thêm Seller Order.
- Tách microservices.
- Thêm Saga.
- Thêm 2PC.
- Thay đổi mô hình dữ liệu lớn.

Chỉ thay đổi schema khi có yêu cầu rõ ràng hoặc phát hiện lỗi thiết kế thực sự.

---

## 11. MULTI-VENDOR ORDER

Một `Order` thuộc về **một Store**.

Nếu Cart có nhiều Store:

```text
Cart
  ├── Store A → Order A
  ├── Store B → Order B
  └── Store C → Order C
```

Checkout phải split cart theo Store trước khi tạo Order.

Không tạo Parent Order/Seller Order chỉ để xử lý multi-vendor.

---

## 12. ADDRESS OWNERSHIP

Khi Buyer tạo Order phải kiểm tra:

```text
address.user_id == current_user.id
```

Foreign Key hợp lệ không đồng nghĩa address thuộc Buyer hiện tại.

Snapshot địa chỉ trong Order phải được giữ để đảm bảo lịch sử.

---

## 13. FLASH SALE ARCHITECTURE

Flash Sale sử dụng:

```text
PostgreSQL
+
Redis
+
Lua Script
```

Redis xử lý stock/concurrency trong thời điểm Flash Sale.

PostgreSQL là nguồn dữ liệu bền vững.

Không coi Redis là database chính thay thế PostgreSQL.

---

## 14. FLASH SALE STOCK MODEL

Có thể tồn tại:

```text
product_variants.stock_quantity
flash_sale_items.allocated_stock
flash_sale_items.available_stock
```

Đây là operational/materialized state phục vụ Flash Sale và không tự động được xem là vi phạm 3NF.

Invariant:

```text
0 <= available_stock <= allocated_stock
```

Seller không được giảm base stock xuống dưới phần stock đang allocate cho Flash Sale đang hoạt động.

---

## 15. FLASH SALE PURCHASE LIMIT

Phải phân biệt:

### Reservation TTL

Dùng để giới hạn thời gian giữ stock của reservation/order chưa hoàn tất.

Ví dụ:

```text
reservation:{orderId}
TTL = 300 seconds
```

### Purchase Limit Lifecycle

Dùng để kiểm soát giới hạn mua của user trong Flash Sale.

Không dùng reservation TTL 300 giây làm lifetime của purchase limit nếu nghiệp vụ yêu cầu giới hạn mua trong toàn bộ Flash Sale.

Purchase-limit state phải có lifecycle riêng.

---

## 16. TIMEOUT ROLLBACK

Khi reservation timeout/cancel phải trả lại **đúng số lượng đã reserve**.

Sai:

```text
INCRBY stock 1
```

Đúng:

```text
INCRBY stock {reservedQuantity}
```

Nếu reserve 5 sản phẩm thì phải trả lại 5.

Rollback phải idempotent để tránh trả stock nhiều lần.

---

## 17. REDIS → DATABASE COMPENSATION

Redis decrement thành công nhưng DB order creation thất bại có thể gây inconsistency.

Flow:

```text
Redis reserve
      ↓
Create DB Order
      ↓
Success
```

Nếu DB fail:

```text
Redis reserve
      ↓
DB fail
      ↓
Compensate Redis
      ↓
Restore exact reserved quantity
```

Không triển khai Saga hoặc 2PC cho scope hiện tại.

Try-Catch compensation phải tránh double compensation.

---

## 18. FLASH SALE ITEM → ORDER ITEM CONSISTENCY

Không tin tưởng hoàn toàn các ID Flash Sale do client gửi.

Service nên load:

```text
FlashSaleItem
```

từ database rồi derive:

```text
slot_id
variant_id
```

từ FlashSaleItem.

Tránh combination client gửi:

```text
slot_id + flash_sale_item_id + variant_id
```

không nhất quán.

Không over-engineer bằng composite FK nếu Service validation đủ cho scope.

---

## 19. FLASH SALE LIFECYCLE

Các operation quan trọng nên idempotent:

- Pre-warm Redis.
- Timeout rollback.
- Sale-end settlement.
- Payment callback.

Invariant:

1. `0 <= available_stock <= allocated_stock`.
2. `allocated_stock` không vượt stock có thể allocate.
3. Seller không phá allocated stock khi cập nhật base stock.
4. Timeout trả đúng reserved quantity.
5. Sale-end settlement chỉ tạo một hiệu ứng.
6. Pre-warm không nhân đôi stock Redis.
7. Redis/DB failure có compensation phù hợp.
8. Purchase limit có lifecycle riêng với reservation TTL.

---

## 20. PAYMENT

Một Order không được có nhiều payment `SUCCESS`.

Nên có partial unique index:

```sql
CREATE UNIQUE INDEX uq_one_success_payment_per_order
ON payments(order_id)
WHERE status = 'SUCCESS';
```

Payment callback phải idempotent.

`transaction_code` phải unique.

---

## 21. WALLET

Wallet transaction phải có khả năng audit.

Không sửa/xóa lịch sử giao dịch tùy tiện.

Các field như:

```text
balance_after
amount
transaction_type
order_id
payment_id
```

phải được sử dụng nhất quán với thiết kế.

---

## 22. VOUCHER

Nếu:

```text
vouchers.store_id IS NOT NULL
```

thì checkout phải kiểm tra:

```text
voucher.store_id == order.store_id
```

Nếu `store_id IS NULL` thì là platform voucher theo thiết kế.

Invariant:

```text
used_quantity <= total_quantity
```

Nếu percentage:

```text
discount_value <= 100
```

Các giá trị tiền/tỷ lệ phải có constraint phù hợp.

---

## 23. ORDER SNAPSHOTS

Giữ các historical snapshot như:

```text
price_at_purchase
product_name
variant_name
recipient_name
recipient_phone
shipping_address
commission_rate
platform_fee
seller_amount
```

Đây là historical snapshot, không xem là lỗi 3NF chỉ vì dữ liệu có thể tồn tại ở bảng khác.

Snapshot quan trọng không nên thay đổi tùy tiện sau khi Order được tạo.

---

## 24. PRODUCT REVIEW

Khi tạo Review phải kiểm tra:

1. User sở hữu Order.
2. Order đã COMPLETED theo nghiệp vụ.
3. `order_item_id` thuộc Order của user.
4. Product của Review khớp Product trong OrderItem.
5. Một OrderItem không bị review nhiều lần nếu `order_item_id` unique.

`product_id` có thể được giữ như intentional denormalization/read optimization.

---

## 25. TRANSACTION

Dùng `@Transactional` cho business operation cần atomicity ở database.

Không giả định `@Transactional` rollback được Redis.

```text
PostgreSQL transaction
≠
Redis transaction
```

Nếu operation liên quan PostgreSQL + Redis, phải có compensation/retry/idempotency phù hợp.

---

## 26. CONCURRENCY

Không dùng flow non-atomic:

```text
read stock
→ if stock > 0
→ decrement
```

trong high-concurrency Flash Sale.

Ưu tiên:

```text
Redis Lua Script
```

cho operation cần atomicity.

Không thêm lock phức tạp nếu Redis/Lua đã giải quyết đúng vấn đề.

---

## 27. N+1

Chú ý N+1 query.

Có thể cân nhắc:

- Fetch join.
- EntityGraph.
- Projection.
- Batch fetching.

Không dùng EAGER bừa bãi để che N+1.

---

## 28. PAGINATION

List API có dữ liệu lớn nên hỗ trợ pagination khi phù hợp.

Không load toàn bộ bảng vào memory nếu không cần.

---

## 29. SECURITY

Không hard-code:

- JWT secret.
- Database password.
- Redis password.
- Payment secret.
- API key.

Dùng environment variables hoặc secret configuration.

JWT authentication/authorization xử lý tại Security layer.

Không bypass Security chỉ để test feature.

---

## 30. MAPSTRUCT + LOMBOK

MapStruct:

```java
@Mapper(componentModel = "spring")
```

Mapper phải được Spring quản lý.

Lombok và MapStruct annotation processing phải được cấu hình đúng.

Không commit generated files nếu convention không yêu cầu.

---

## 31. VALIDATION

API input dùng Jakarta Validation khi phù hợp:

```java
@NotNull
@NotBlank
@Positive
@PositiveOrZero
@Size
@Email
```

Business validation phức tạp nằm trong Service.

---

## 32. API CONVENTION

REST API sử dụng HTTP method đúng mục đích:

```text
GET
POST
PUT
PATCH
DELETE
```

Không dùng POST cho mọi operation nếu HTTP semantic có method phù hợp.

---

## 33. TESTING

Sau mỗi task:

1. Build project.
2. Chạy test liên quan.
3. Nếu liên quan DB, kiểm tra migration.
4. Nếu liên quan Redis/concurrency, kiểm tra atomicity và edge cases.
5. Review diff.

### BẮT BUỘC

Trước build/test/run:

```cmd
java25
java -version
```

Sau đó:

```cmd
gradlew clean build
```

hoặc:

```cmd
gradlew test
```

Nếu `java -version` không phải Java 25 thì dừng và báo lỗi.

---

## 34. GIT WORKFLOW

Branch:

```text
feature/...
fix/...
refactor/...
chore/...
docs/...
```

Commit ngắn gọn, đúng thay đổi.

Ví dụ:

```text
feat: implement flash sale reservation
fix: restore exact reserved stock on timeout
chore: initialize project structure
```

Không commit:
- `.env`
- secret
- password
- IDE files không cần thiết
- build output

---

## 35. NO PREMATURE OVER-ENGINEERING

Không tự ý thêm:

- Microservices.
- Kafka.
- Saga.
- 2PC.
- CQRS.
- Event Sourcing.
- Kubernetes.
- Distributed Lock phức tạp.
- Message broker.
- Parent Order.
- Seller Order.

nếu chưa có requirement rõ ràng.

Mục tiêu là hệ thống Flash Sale B2C có kiến trúc rõ ràng, đúng nghiệp vụ và triển khai/test được trong scope đồ án.

---

## 36. IDEMPOTENCY

Các operation có retry phải được thiết kế để retry không tạo side effect duplicate.

Đặc biệt:

- Payment callback.
- Stock rollback.
- Flash Sale settlement.
- Redis pre-warm.
- Order creation trong flow có retry.

---

## 37. BACKGROUND JOBS

Scheduled job phải:

- Có mục đích rõ ràng.
- Có cơ chế tránh duplicate nếu cần.
- Có log.
- Có idempotency.
- Không âm thầm thay đổi dữ liệu ngoài business rule.

---

## 38. CODE QUALITY

Ưu tiên:

- Code dễ đọc.
- Method có trách nhiệm rõ ràng.
- Tên biến/method/class có ý nghĩa.
- Không duplicate logic.
- Không tạo abstraction chỉ vì "có thể dùng sau".
- Không viết code quá phức tạp cho business rule đơn giản.

---

## 39. DOCUMENTATION CONSISTENCY

Khi implementation thay đổi:

- Không tự ý sửa docs.
- Kiểm tra implementation phù hợp docs.
- Nếu có mâu thuẫn lớn, báo user trước khi đổi architecture.

Code, migration và API implementation phải nhất quán với thiết kế đã chốt.

---

## 40. TASK EXECUTION CHECKLIST

### Understanding
- [ ] Đã đọc requirement liên quan.
- [ ] Đã đọc docs liên quan.
- [ ] Đã kiểm tra code hiện tại.

### Implementation
- [ ] Đúng module.
- [ ] Đúng layer.
- [ ] Không business logic trong Controller.
- [ ] Không business logic trong Repository.
- [ ] Mapper đúng convention.

### Database
- [ ] Constraint đúng.
- [ ] FK đúng.
- [ ] Index phù hợp.
- [ ] Migration được tạo nếu cần.
- [ ] Không sửa docs ngoài yêu cầu.

### Flash Sale
- [ ] Redis operation atomic.
- [ ] Lua script đúng.
- [ ] Reservation TTL tách purchase limit.
- [ ] Rollback trả đúng quantity.
- [ ] Có compensation khi DB fail.
- [ ] Lifecycle operation idempotent.

### Security
- [ ] Không hard-code secret.
- [ ] Authorization đúng.
- [ ] Ownership check đúng.

### Validation
- [ ] Input validation.
- [ ] Business validation.
- [ ] Edge cases.

### Build/Test
- [ ] Đã chạy `java25` trước build/test/run.
- [ ] `java -version` xác nhận Java 25.
- [ ] Build/test thành công hoặc đã báo rõ lỗi.
- [ ] Đã review diff.

---

## 41. REQUIRED FINAL RESPONSE FORMAT

Sau khi hoàn thành task:

```text
## Changes
- ...

## Implementation
- ...

## Database
- ...

## Tests
- Java version: ...
- Command: ...
- Result: ...

## Notes
- ...
```

Nếu có lỗi:

```text
## Issues
- ...
```

Không tuyên bố:

- "perfect"
- "100% safe"
- "0% overselling"
- "guaranteed"
- "10/10"

Phải mô tả chính xác phạm vi đã kiểm tra, kết quả test và limitation còn lại.

---

## 42. AUTH & USER MODULE CONVENTIONS & ENDPOINTS

### 1. JWT Configuration & Security
- **Header format**: `Authorization: Bearer <access_token>`
- **Signing Algorithm**: HMAC-SHA256 (HS256).
- **Secret requirements**: Tối thiểu 32 ký tự (256-bit). Cấu hình qua `jwt.secret` (hoặc biến môi trường `JWT_SECRET`).
- **Profile local**: Đã cấu hình tại `application-local.yaml` với secret 64 ký tự.
- **Expiration**:
  - Access Token: `jwt.expiration-ms` (mặc định 3,600,000 ms = 1 giờ).
  - Refresh Token: `jwt.refresh-expiration-ms` (mặc định 604,800,000 ms = 7 ngày).
- **Stateless Authentication**: Sử dụng `JwtAuthenticationFilter`, trả về 401 Unauthorized khi token invalid/expired, 403 Forbidden khi thiếu quyền.

### 2. RBAC Authorities Mapping
- **Role Authority**: `ROLE_<ROLE_NAME>` (ví dụ `ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`).
- **Permission Authority**: Atomic code (ví dụ `order:create`, `product:read`).
- **Feature Flag**: Chỉ cấp permission nếu `permissions.is_active == true`.
- **Principal Context**: `CustomUserDetails` chứa `AuthAccount` (bao gồm `id`, `username`, `email`). Truy xuất ID người dùng hiện tại qua `customUserDetails.getId()`.

### 3. Address Ownership & Invariants
- **Ownership check bắt buộc**: Mọi thao tác trên address (`GET /addresses/{id}`, `PUT`, `DELETE`, `PATCH default`) phải xác nhận:
  ```text
  address.getUser().getId().equals(currentUserId)
  ```
  Nếu không khớp, ném lỗi 403 `ADDRESS_ACCESS_DENIED`.
- **Single Default Address**: Khi đánh dấu một địa chỉ là mặc định (`is_default = true`), toàn bộ các địa chỉ khác của user đó phải chuyển về `is_default = false`.
- **XOR Constraint**: Địa chỉ của user phải gán `user = currentUser`, `store = null` để thỏa mãn CHECK constraint `chk_address_owner_xor` ở DB.

### 4. API Endpoints List

#### Auth Endpoints (`/api/v1/auth`)
| Method | Endpoint | Access | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Public | Đăng ký tài khoản Buyer mới (`username`, `email`, `password`, `fullName`, `phoneNumber`). |
| `POST` | `/api/v1/auth/login` | Public | Đăng nhập bằng `usernameOrEmail` + `password`. Trả về `accessToken`, `refreshToken`, user info và roles. |
| `POST` | `/api/v1/auth/refresh-token` | Public | Cấp lại `accessToken` mới từ `refreshToken` hợp lệ. |
| `POST` | `/api/v1/auth/logout` | Public | Logout client-side (vô hiệu hóa token phía client). |

#### User Profile Endpoints (`/api/v1/users`)
| Method | Endpoint | Access | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users/me` | Authenticated | Lấy thông tin profile người dùng hiện tại. |
| `PUT` | `/api/v1/users/me` | Authenticated | Cập nhật thông tin profile (`fullName`, `phoneNumber`, `avatarUrl`). |
| `PUT` | `/api/v1/users/me/change-password` | Authenticated | Đổi mật khẩu (`oldPassword`, `newPassword`), xác thực mật khẩu cũ qua BCrypt. |

#### User Address Endpoints (`/api/v1/users/addresses`)
| Method | Endpoint | Access | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users/addresses` | Authenticated | Lấy danh sách địa chỉ của người dùng hiện tại. |
| `POST` | `/api/v1/users/addresses` | Authenticated | Thêm mới địa chỉ nhận hàng. |
| `GET` | `/api/v1/users/addresses/{id}` | Authenticated (Owner) | Lấy chi tiết một địa chỉ (kiểm tra ownership). |
| `PUT` | `/api/v1/users/addresses/{id}` | Authenticated (Owner) | Cập nhật địa chỉ (kiểm tra ownership). |
| `DELETE` | `/api/v1/users/addresses/{id}` | Authenticated (Owner) | Xóa địa chỉ (kiểm tra ownership). |
| `PATCH` | `/api/v1/users/addresses/{id}/default` | Authenticated (Owner) | Đặt địa chỉ làm mặc định (kiểm tra ownership, reset các địa chỉ khác). |
