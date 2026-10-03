package com.b2c.flash_sale_b2c_UTC2.voucher.dto;

import jakarta.validation.constraints.NotBlank;
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
public class ApplyVoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    private Long storeId; // store sở hữu đơn hàng (nếu áp dụng mã shop)

    @NotNull(message = "Giá trị đơn hàng không được để trống")
    @Positive(message = "Giá trị đơn hàng phải lớn hơn 0")
    private BigDecimal subtotalAmount;
}
