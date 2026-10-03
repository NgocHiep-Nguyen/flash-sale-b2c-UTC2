package com.b2c.flash_sale_b2c_UTC2.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutStoreOrderRequest {

    @NotNull(message = "ID gian hàng không được để trống")
    private Long storeId;

    @NotEmpty(message = "Danh sách sản phẩm trong đơn hàng gian hàng không được để trống")
    @Valid
    private List<CheckoutItemRequest> items;

    private String voucherCode;
}
