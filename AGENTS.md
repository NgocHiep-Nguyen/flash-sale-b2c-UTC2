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

### 5. Store & Warehouse Address Endpoints (`/api/v1/stores` & `/api/v1/admin/stores`)
| Method | Endpoint | Access | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/stores` | Authenticated | Đăng ký mở Store mới (mặc định trạng thái `PENDING`, tự động tạo Wallet 1-1). |
| `GET` | `/api/v1/stores/me` | Authenticated (Seller) | Lấy thông tin Store của người dùng hiện tại. |
| `PUT` | `/api/v1/stores/me` | Authenticated (Seller) | Cập nhật thông tin Store (`storeName`, `logoUrl`, `description`). |
| `GET` | `/api/v1/stores/{id}` | Public | Lấy thông tin công khai của một Store theo ID. |
| `PATCH` | `/api/v1/admin/stores/{id}/status` | Admin | Duyệt/Khóa Store. Khi duyệt `APPROVED`, tự động gán role `SELLER` cho chủ shop. |
| `GET` | `/api/v1/stores/me/addresses` | Authenticated (Seller) | Danh sách địa chỉ kho lấy hàng của Store. |
| `POST` | `/api/v1/stores/me/addresses` | Authenticated (Seller) | Thêm mới địa chỉ kho (thỏa mãn ràng buộc XOR: `user=null`, `store=store`). |
| `PUT` | `/api/v1/stores/me/addresses/{id}` | Authenticated (Seller) | Cập nhật địa chỉ kho. |
| `DELETE` | `/api/v1/stores/me/addresses/{id}` | Authenticated (Seller) | Xóa địa chỉ kho của Store. |
| `PATCH` | `/api/v1/stores/me/addresses/{id}/default` | Authenticated (Seller) | Đặt địa chỉ kho làm mặc định. |

### 6. Category Endpoints (`/api/v1/categories` & `/api/v1/admin/categories`)
| Method | Endpoint | Access | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/categories` | Public | Danh sách cây ngành hàng hiển thị công khai. |
| `GET` | `/api/v1/categories/{id}` | Public | Chi tiết ngành hàng theo ID. |
| `POST` | `/api/v1/admin/categories` | Admin | Tạo mới ngành hàng (`name`, `slug`, `parentId`, `imageUrl`). |
| `PUT` | `/api/v1/admin/categories/{id}` | Admin | Cập nhật ngành hàng. |
| `DELETE` | `/api/v1/admin/categories/{id}` | Admin | Xóa ngành hàng (chặn nếu đang có ngành hàng con). |

### 7. Product SPU-SKU Module Endpoints (`/api/v1/products` & `/api/v1/seller/products`)
| Method | Endpoint | Access | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/products` | Public | Tìm kiếm, lọc sản phẩm (categoryId, keyword, minPrice, maxPrice), phân trang `PageResponse`. Chỉ trả về product `ACTIVE` của store `APPROVED`. Không bị N+1 nhờ batch fetch variants. |
| `GET` | `/api/v1/products/{id}` | Public | Chi tiết sản phẩm SPU và danh sách SKU `ACTIVE` (chỉ hiển thị nếu product `ACTIVE` và store `APPROVED`). |
| `POST` | `/api/v1/seller/products` | Seller | Đăng bán sản phẩm mới SPU-SKU. Kiểm tra store `APPROVED`, validate giá > 0, tồn kho >= 0, SKU unique, số SKU khớp `tier_variation_configs`. |
| `GET` | `/api/v1/seller/products` | Seller | Lấy danh sách sản phẩm thuộc gian hàng của seller (phân trang, lọc theo status). |
| `GET` | `/api/v1/seller/products/{id}` | Seller | Chi tiết sản phẩm và toàn bộ biến thể SKU thuộc gian hàng seller (kiểm tra quyền sở hữu store). |
| `PUT` | `/api/v1/seller/products/{id}` | Seller | Cập nhật SPU và SKUs. Chặn sửa giá hoặc giảm tồn kho nếu SKU đang trong Flash Sale `ACTIVE`. Bảo vệ dữ liệu bằng Khóa Lạc Quan (`@Version` / `V2__`). |
| `DELETE` | `/api/v1/seller/products/{id}` | Seller | Xóa sản phẩm. Tự động **Soft Delete** (`status = INACTIVE`) nếu SKU đã có trong `order_items` hoặc `flash_sale_items`; **Hard Delete** nếu chưa phát sinh đơn hàng. |
| `PATCH` | `/api/v1/seller/products/{id}/status` | Seller | Đổi trạng thái sản phẩm (`ACTIVE`, `INACTIVE`, `OUT_OF_STOCK`). |

### 8. Flash Sale Core Module Endpoints & Concurrency Architecture
- **Boundary rule**: Chỉ thuộc module `flashsale`. Các module `order`, `store`, `voucher`, `cart`, `payment`, `wallet` là read-only.
- **Tích hợp Module Order**: Thông qua interface `FlashSaleOrderPort` (`createPendingOrder`, `lockExpiredPendingOrders`, `cancelTimeoutIfPending`, `countPendingBySlot`).
- **Atomic Reservation**: Lua script (`reserve_stock.lua`) trên Redis với key `flash_sale:stock:{itemId}` và `flash_sale:user_limit:{slotId}:{userId}:{itemId}`. Hỗ trợ pluggable `StockReservationStrategy` (Lua script mặc định, Redisson distributed lock so sánh).
- **Dual-write Compensation**: Khi gọi OrderPort hoặc DB thất bại, tự động kích hoạt bù hoàn Redis (`INCRBY stock`, `DECRBY user_limit`) và DB (`replenishAvailableStockConditionally`).
- **Jobs nền (Schedulers)**:
  - `processExpiredReservations` (15s): Quét đơn `PENDING_PAYMENT` quá hạn 300s, chuyển `CANCELLED_TIMEOUT` nguyên tử, hoàn trả chính xác số lượng về Redis và DB.
  - `processEndedSlotsAndReturnUnsoldStock` (30s): Đóng phiên hết hạn, tự động hoàn trả tồn kho chưa bán (`available_stock`) về kho gốc biến thể `product_variants.stock_quantity`.

#### Flash Sale Admin Endpoints (`/api/v1/admin/flash-sales`)
| Method | Endpoint | Access | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/admin/flash-sales/slots` | Admin | Tạo khung giờ Flash Sale mới (kiểm tra chống overlap thời gian). |
| `PUT` | `/api/v1/admin/flash-sales/slots/{id}` | Admin | Cập nhật khung giờ Flash Sale (chặn cập nhật phiên đã `ENDED`, kiểm tra overlap). |
| `GET` | `/api/v1/admin/flash-sales/slots/{id}` | Admin | Xem chi tiết khung giờ Flash Sale. |
| `GET` | `/api/v1/admin/flash-sales/slots` | Admin | Xem danh sách toàn bộ khung giờ Flash Sale. |
| `PATCH` | `/api/v1/admin/flash-sales/items/{id}/approve` | Admin | Duyệt SKU đăng ký vào Flash Sale, khấu trừ kho gốc `product_variants.stock_quantity` nguyên tử. |
| `POST` | `/api/v1/admin/flash-sales/slots/{id}/pre-warm` | Admin | Pre-warm nạp tồn kho lên Redis bằng `SETNX` (idempotent, tính kèm đơn pending). |

#### Flash Sale Seller Endpoints (`/api/v1/seller/flash-sales`)
| Method | Endpoint | Access | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/seller/flash-sales/items` | Seller | Đăng ký biến thể SKU tham gia Flash Sale (`flash_sale_price < original_price`, `allocated_stock <= stock_quantity`). |

#### Flash Sale Public & Reservation Endpoints (`/api/v1/flash-sales`)
| Method | Endpoint | Access | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/flash-sales/slots` | Public | Danh sách phiên sale đang/sắp diễn ra kèm tồn kho thời gian thực từ Redis. |
| `POST` | `/api/v1/flash-sales/reservations` | Authenticated | Đặt hàng giữ chỗ Flash Sale với `Idempotency-Key`, thực thi Lua script, tạo đơn `PENDING_PAYMENT` qua `FlashSaleOrderPort`. |

---

## 43. REALTIME WEBSOCKET (STOMP + SOCKJS)

### 1. Tổng quan

Realtime channel phục vụ push event cho client (stock update, slot status, order result). Không thay thế HTTP REST — chỉ bổ sung lớp notification.

**Stack**: Spring WebSocket + STOMP + SockJS.

**Endpoint handshake**: `ws://host/ws` (SockJS fallback: `http://host/ws/...`).

### 2. Cấu hình chính

| File | Vai trò |
| :--- | :--- |
| `config/realtime/WebSocketConfig.java` | Broker prefix `/topic`, `/user`; heartbeat 10s |
| `config/realtime/HandshakeAuthInterceptor.java` | Đọc JWT từ `?token=` query param |
| `config/realtime/WebSocketAuthConfig.java` | Reload authorities qua `UserDetailsService` tại CONNECT |
| `config/realtime/WebSocketSchedulerConfig.java` | Dedicated `TaskScheduler` cho STOMP heartbeat |
| `config/realtime/WsSessionEventListener.java` | Log lifecycle events + đếm active sessions |
| `config/CorsConfig.java` | CORS mapping `/ws/**` |
| `config/SecurityConfig.java` | `permitAll()` cho `/ws/**` (auth xử lý trong interceptor) |

### 3. Auth flow (browser → server)

```text
Browser:  new WebSocket('/ws?token=<JWT>')
           │
           ▼
HandshakeAuthInterceptor
           │ validate JWT, set Principal vào session attributes
           ▼
WebSocket connected → STOMP CONNECT frame
           │
           ▼
WebSocketAuthConfig (ChannelInterceptor preSend)
           │ reload UserDetails, gán vào STOMP accessor
           ▼
Authenticated session — sẵn sàng SUBSCRIBE / SEND
```

Lý do dùng query param thay vì header: browser không cho set `Authorization` khi upgrade WebSocket.

### 4. WS Destinations (single source of truth)

Mọi URL pattern phải build qua class `flashsale.realtime.WsDestinations`. Không hard-code URL rải rác trong service.

#### Public topic (broadcast, ai cũng subscribe được):

| Method | Destination | Mô tả |
| :--- | :--- | :--- |
| `itemStock(itemId)` | `/topic/flash-sale/item/{itemId}/stock` | Stock realtime cho 1 SKU |
| `slotStockUpdate(slotId)` | `/topic/flash-sale/slot/{slotId}/stock-update` | Stock thay đổi trong slot |
| `slotStatus(slotId)` | `/topic/flash-sale/slot/{slotId}/status` | Slot chuyển trạng thái |

#### Private queue (per-user, Spring prefix `/user/{username}`):

| Method | Destination (resolved) | Mô tả |
| :--- | :--- | :--- |
| `reservationResult()` | `/user/{username}/queue/flash-sale/reservation-result` | Kết quả reservation riêng user |
| `orderUpdates(orderCode)` | `/user/{username}/queue/flash-sale/orders/{orderCode}/updates` | Update riêng cho 1 đơn |

### 5. Payload format

Tất cả event đều wrap trong `ApiResponse<T>` chung của project. Body payload là `FlashSaleWsEvent`:

```java
FlashSaleWsEvent {
  EventType eventType;          // STOCK_DECREMENTED, ORDER_RESERVED, ...
  Long slotId, flashSaleItemId, orderId, userId;
  Integer availableStock, allocatedStock, quantity, restoredQuantity;
  Long totalAmount;
  String orderCode, slotStatus;
  Instant expiresAt, occurredAt;
}

enum EventType {
  STOCK_DECREMENTED,
  STOCK_RESTORED,
  STOCK_RETURNED_UNSOLD,
  SLOT_ACTIVATED,
  SLOT_CLOSED,
  ORDER_RESERVED,
  ORDER_CANCELLED_TIMEOUT
}
```

### 6. Broadcaster layer

`FlashSaleWsBroadcaster` là service duy nhất gọi `SimpMessagingTemplate`. Mọi service khác muốn push realtime phải inject broadcaster, không gọi trực tiếp `convertAndSend`.

Các method chính:
- `broadcastStockUpdate(slotId, itemId, availableStock)`
- `broadcastStockRestored(slotId, itemId, availableStock, restoredQuantity)`
- `broadcastUnsoldStockReturned(slotId, itemId, availableStock)`
- `broadcastSlotStatus(slotId, newStatus)`
- `sendReservationResultToUser(username, event)`
- `sendOrderCancelledToUser(username, event)`

### 7. Quy tắc khi dùng WS trong business flow

- WS **chỉ là thông báo**. Không để client quyết định logic dựa trên WS message.
- Stock đếm vẫn lấy từ Redis (`GET flash_sale:stock:{itemId}`) làm source of truth.
- DB không phụ thuộc WS — nếu WS message bị miss (network), client vẫn get đúng stock qua REST.
- Không queue message khi user offline (`SimpMessagingTemplate` mặc định).
- Heartbeat 10s cả 2 chiều, dedicated scheduler pool (xem `application-websocket.yaml`).

