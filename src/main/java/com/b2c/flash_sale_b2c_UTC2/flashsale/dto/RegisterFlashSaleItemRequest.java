package com.b2c.flash_sale_b2c_UTC2.flashsale.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterFlashSaleItemRequest {

    @NotNull(message = "ID khung giờ Flash Sale không được để trống")
    private Long slotId;

    @NotNull(message = "ID biến thể SKU không được để trống")
    private Long variantId;

    @NotNull(message = "Giá Flash Sale không được để trống")
    @Positive(message = "Giá Flash Sale phải lớn hơn 0")
    private BigDecimal flashSalePrice;

    @NotNull(message = "Số lượng tồn kho phân bổ không được để trống")
    @Positive(message = "Số lượng tồn kho phân bổ phải lớn hơn 0")
    private Integer allocatedStock;

    @Positive(message = "Giới hạn mua của khách hàng phải lớn hơn 0")
    @Builder.Default
    private Integer userPurchaseLimit = 1;

    private BigDecimal commissionRateOverride;
}
