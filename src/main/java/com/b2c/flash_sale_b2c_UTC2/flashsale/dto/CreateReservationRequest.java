package com.b2c.flash_sale_b2c_UTC2.flashsale.dto;

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
public class CreateReservationRequest {

    @NotNull(message = "ID mục Flash Sale không được để trống")
    private Long flashSaleItemId;

    @NotNull(message = "ID địa chỉ nhận hàng không được để trống")
    private Long addressId;

    @NotNull(message = "Số lượng đặt mua không được để trống")
    @Positive(message = "Số lượng đặt mua phải lớn hơn 0")
    @Builder.Default
    private Integer quantity = 1;
}
