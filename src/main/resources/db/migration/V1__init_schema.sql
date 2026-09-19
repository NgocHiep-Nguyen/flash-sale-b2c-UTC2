-- ===================================================================
-- Flash Sale B2C - UTC2 Database Migration V1: Initial Schema
-- Chuẩn hóa CSDL 24 bảng cấp 3NF theo thiết kế docs/03_system_design.md
-- ===================================================================

-- -------------------------------------------------------------------
-- PHÂN HỆ 1: XÁC THỰC, PHÂN QUYỀN MA TRẬN & ĐỊA CHỈ
-- -------------------------------------------------------------------

-- 1. Bảng roles (Danh mục vai trò người dùng)
CREATE TABLE roles (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng users (Tài khoản người dùng toàn sàn)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) UNIQUE,
    avatar_url VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng permission_groups (Nhóm quyền chức năng độc lập)
CREATE TABLE permission_groups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bảng permissions (Danh mục quyền nguyên tử kèm cờ khóa tính năng)
CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    module VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 5. Bảng group_permissions (Bảng nối Nhóm quyền - Quyền nguyên tử)
CREATE TABLE group_permissions (
    group_id INT NOT NULL REFERENCES permission_groups(id) ON DELETE CASCADE,
    permission_id INT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, permission_id)
);

-- 6. Bảng user_roles (Bảng trung gian phân quyền ma trận)
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id SMALLINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_group_id INT REFERENCES permission_groups(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_roles_matrix UNIQUE NULLS NOT DISTINCT (user_id, role_id, permission_group_id)
);

-- 7. Bảng auth_accounts (Định danh đăng nhập OAuth2)
CREATE TABLE auth_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    provider_account_id VARCHAR(100) NOT NULL,
    provider_email VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_auth_provider_account UNIQUE (provider, provider_account_id)
);

-- -------------------------------------------------------------------
-- PHÂN HỆ 2: GIAN HÀNG & VÍ DOANH THU CỦA SELLER
-- -------------------------------------------------------------------

-- 8. Bảng stores (Gian hàng của Người bán)
CREATE TABLE stores (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    store_name VARCHAR(150) NOT NULL UNIQUE,
    logo_url VARCHAR(255),
    description TEXT,
    default_commission_rate DECIMAL(5,4) DEFAULT 0.0500,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 9. Bảng addresses (Sổ địa chỉ chuẩn hóa 2 Khóa ngoại vật lý kèm tọa độ GPS)
CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    store_id BIGINT REFERENCES stores(id) ON DELETE CASCADE,
    contact_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    province VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    ward VARCHAR(100) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_addresses_owner CHECK (
        (user_id IS NOT NULL AND store_id IS NULL) OR
        (user_id IS NULL AND store_id IS NOT NULL)
    )
);

CREATE INDEX idx_addresses_user ON addresses(user_id);
CREATE INDEX idx_addresses_store ON addresses(store_id);

-- 10. Bảng wallets (Ví doanh thu của Gian hàng)
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL UNIQUE REFERENCES stores(id),
    balance DECIMAL(15,2) DEFAULT 0.00 CHECK (balance >= 0),
    frozen_balance DECIMAL(15,2) DEFAULT 0.00 CHECK (frozen_balance >= 0),
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------------------
-- PHÂN HỆ 3: DANH MỤC & KHO HÀNG GỐC THEO MÔ HÌNH SPU - SKU
-- -------------------------------------------------------------------

-- 11. Bảng categories (Ngành hàng sản phẩm)
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    commission_rate DECIMAL(5,4) DEFAULT 0.0500,
    description VARCHAR(255)
);

-- 12. Bảng products (Thông tin sản phẩm gốc - SPU)
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id),
    category_id INT NOT NULL REFERENCES categories(id),
    name VARCHAR(255) NOT NULL,
    image_url VARCHAR(255),
    description TEXT,
    tier_variation_configs JSONB,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 13. Bảng product_variants (Chi tiết từng biến thể phân loại - SKU)
CREATE TABLE product_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(50) NOT NULL UNIQUE,
    variant_name VARCHAR(150) NOT NULL,
    attributes JSONB,
    original_price DECIMAL(15,2) NOT NULL CHECK (original_price > 0),
    stock_quantity INT NOT NULL CHECK (stock_quantity >= 0),
    image_url VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_variants_product_sku UNIQUE (product_id, sku)
);

CREATE INDEX idx_product_variants_product ON product_variants(product_id);

-- -------------------------------------------------------------------
-- PHÂN HỆ 4: KHUNG GIỜ & SẢN PHẨM FLASH SALE (CỐT LÕI)
-- -------------------------------------------------------------------

-- 14. Bảng flash_sale_slots (Khung giờ Flash Sale do Admin mở)
CREATE TABLE flash_sale_slots (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    reservation_ttl_seconds INT DEFAULT 300 CHECK (reservation_ttl_seconds > 0),
    status VARCHAR(20) DEFAULT 'UPCOMING',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_flash_sale_slots_time CHECK (end_time > start_time)
);

-- 15. Bảng flash_sale_items (Biến thể SKU tham gia Flash Sale - Chuẩn 3NF)
CREATE TABLE flash_sale_items (
    id BIGSERIAL PRIMARY KEY,
    slot_id BIGINT NOT NULL REFERENCES flash_sale_slots(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    flash_sale_price DECIMAL(15,2) NOT NULL CHECK (flash_sale_price > 0),
    allocated_stock INT NOT NULL CHECK (allocated_stock > 0),
    available_stock INT NOT NULL,
    user_purchase_limit INT DEFAULT 1 CHECK (user_purchase_limit > 0),
    commission_rate_override DECIMAL(5,4),
    status VARCHAR(20) DEFAULT 'PENDING_APPROVAL',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_flash_sale_items_slot_variant UNIQUE (slot_id, variant_id),
    CONSTRAINT chk_flash_sale_items_stock CHECK (available_stock >= 0 AND available_stock <= allocated_stock)
);

-- -------------------------------------------------------------------
-- PHÂN HỆ 7: MÃ GIẢM GIÁ & KHUYẾN MÃI
-- -------------------------------------------------------------------

-- 16. Bảng vouchers (Mã giảm giá Khuyến mãi)
CREATE TABLE vouchers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    store_id BIGINT REFERENCES stores(id),
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(15,2) NOT NULL CHECK (discount_value > 0),
    min_order_amount DECIMAL(15,2) DEFAULT 0.00 CHECK (min_order_amount >= 0),
    max_discount_amount DECIMAL(15,2),
    total_quantity INT NOT NULL CHECK (total_quantity > 0),
    used_quantity INT DEFAULT 0 CHECK (used_quantity >= 0),
    user_usage_limit INT DEFAULT 1 CHECK (user_usage_limit > 0),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_vouchers_time CHECK (end_time > start_time),
    CONSTRAINT chk_vouchers_quota CHECK (used_quantity <= total_quantity),
    CONSTRAINT chk_vouchers_percent CHECK (discount_type != 'PERCENT' OR discount_value <= 100.00)
);

-- -------------------------------------------------------------------
-- PHÂN HỆ 5: ĐƠN HÀNG & THANH TOÁN ĐA CỔNG
-- -------------------------------------------------------------------

-- 17. Bảng orders (Đơn đặt hàng & Giữ chỗ có thời hạn)
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_code VARCHAR(50) NOT NULL UNIQUE,
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    store_id BIGINT NOT NULL REFERENCES stores(id),
    slot_id BIGINT REFERENCES flash_sale_slots(id),
    voucher_id BIGINT REFERENCES vouchers(id),
    shipping_address_id BIGINT REFERENCES addresses(id) ON DELETE SET NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(15) NOT NULL,
    shipping_address_text TEXT NOT NULL,
    subtotal_amount DECIMAL(15,2) NOT NULL CHECK (subtotal_amount > 0),
    voucher_discount_amount DECIMAL(15,2) DEFAULT 0.00 CHECK (voucher_discount_amount >= 0),
    total_amount DECIMAL(15,2) NOT NULL CHECK (total_amount > 0),
    commission_rate DECIMAL(5,4) NOT NULL,
    platform_fee DECIMAL(15,2) NOT NULL,
    seller_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 18. Bảng order_items (Chi tiết mặt hàng trong đơn - Chuẩn 3NF)
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    flash_sale_item_id BIGINT REFERENCES flash_sale_items(id),
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    product_name VARCHAR(255) NOT NULL,
    variant_name VARCHAR(150) NOT NULL,
    price_at_purchase DECIMAL(15,2) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0)
);

-- 19. Bảng payments (Lịch sử thanh toán Cổng: ZaloPay QR, COD & Hoàn tiền)
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    payment_method VARCHAR(30) NOT NULL,
    transaction_code VARCHAR(100) NOT NULL UNIQUE,
    gateway_trans_id VARCHAR(100),
    amount DECIMAL(15,2) NOT NULL,
    qr_code_data TEXT,
    payment_url TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    callback_payload JSONB,
    paid_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    refund_reason VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_one_success_payment_per_order ON payments(order_id) WHERE status = 'SUCCESS';

-- -------------------------------------------------------------------
-- PHÂN HỆ 6: SỔ CÁI MINH BẠCH TÀI CHÍNH
-- -------------------------------------------------------------------

-- 20. Bảng wallet_transactions (Sổ cái đối soát tài chính)
CREATE TABLE wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL REFERENCES wallets(id),
    order_id BIGINT REFERENCES orders(id),
    payment_id BIGINT REFERENCES payments(id),
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 21. Bảng voucher_usages (Lịch sử sử dụng Voucher)
CREATE TABLE voucher_usages (
    id BIGSERIAL PRIMARY KEY,
    voucher_id BIGINT NOT NULL REFERENCES vouchers(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id),
    discount_amount DECIMAL(15,2) NOT NULL CHECK (discount_amount > 0),
    used_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------------------
-- PHÂN HỆ 8: GIỎ HÀNG & MUA SẮM TIÊU CHUẨN
-- -------------------------------------------------------------------

-- 22. Bảng carts (Giỏ hàng cá nhân của Người mua)
CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 23. Bảng cart_items (Chi tiết từng món trong giỏ hàng)
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cart_items_cart_variant UNIQUE (cart_id, variant_id)
);

-- -------------------------------------------------------------------
-- PHÂN HỆ 9: ĐÁNH GIÁ & PHẢN HỒI SẢN PHẨM
-- -------------------------------------------------------------------

-- 24. Bảng product_reviews (Đánh giá & Phản hồi Khách hàng)
CREATE TABLE product_reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    order_item_id BIGINT NOT NULL UNIQUE REFERENCES order_items(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    image_urls JSONB,
    seller_reply TEXT,
    seller_reply_at TIMESTAMPTZ,
    status VARCHAR(20) DEFAULT 'VISIBLE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_reviews_product ON product_reviews(product_id, status);
CREATE INDEX idx_product_reviews_user ON product_reviews(user_id);
