package com.b2c.flash_sale_b2c_UTC2.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderCode;
    private Long buyerId;
    private Long storeId;
    private Long slotId;
    private Long voucherId;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddressText;
    private BigDecimal subtotalAmount;
    private BigDecimal voucherDiscountAmount;
    private BigDecimal totalAmount;
    private BigDecimal commissionRate;
    private BigDecimal platformFee;
    private BigDecimal sellerAmount;
    private String status;
    private Instant expiresAt;
    private Instant createdAt;
}
