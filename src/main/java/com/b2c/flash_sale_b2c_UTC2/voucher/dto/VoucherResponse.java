package com.b2c.flash_sale_b2c_UTC2.voucher.dto;

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
public class VoucherResponse {
    private Long id;
    private String code;
    private Long storeId;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer totalQuantity;
    private Integer usedQuantity;
    private Integer userUsageLimit;
    private Instant startTime;
    private Instant endTime;
    private String status;
    private Instant createdAt;
}
