package com.b2c.flash_sale_b2c_UTC2.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutItemRequest {

    @NotNull(message = "ID biến thể sản phẩm không được để trống")
    private Long variantId;

    @NotNull(message = "Số lượng mua không được để trống")
    @Positive(message = "Số lượng mua phải lớn hơn 0")
    private Integer quantity;
}
