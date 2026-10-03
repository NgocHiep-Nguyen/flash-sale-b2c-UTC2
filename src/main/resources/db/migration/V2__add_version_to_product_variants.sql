-- V2__add_version_to_product_variants.sql
-- Thêm cột version phục vụ Khóa Lạc Quan (@Version) trong JPA nhằm ngăn ngừa mất mát dữ liệu đồng thời

ALTER TABLE product_variants
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
