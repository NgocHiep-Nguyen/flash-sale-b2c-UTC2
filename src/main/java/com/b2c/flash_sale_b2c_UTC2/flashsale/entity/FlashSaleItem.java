package com.b2c.flash_sale_b2c_UTC2.flashsale.entity;

import com.b2c.flash_sale_b2c_UTC2.product.entity.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity đại diện cho bảng flash_sale_items (Biến thể SKU tham gia Flash Sale - Chuẩn 3NF).
 */
@Entity
@Table(name = "flash_sale_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private FlashSaleSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "flash_sale_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal flashSalePrice;

    @Column(name = "allocated_stock", nullable = false)
    private Integer allocatedStock;

    @Column(name = "available_stock", nullable = false)
    private Integer availableStock;

    @Column(name = "user_purchase_limit")
    @Builder.Default
    private Integer userPurchaseLimit = 1;

    @Column(name = "commission_rate_override", precision = 5, scale = 4)
    private BigDecimal commissionRateOverride;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PENDING_APPROVAL";

    @Column(name = "created_at")
    private Instant createdAt;
}
