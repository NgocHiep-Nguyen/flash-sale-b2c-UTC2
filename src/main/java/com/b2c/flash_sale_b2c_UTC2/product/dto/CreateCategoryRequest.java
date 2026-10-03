package com.b2c.flash_sale_b2c_UTC2.product.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {

    @NotBlank(message = "Tên ngành hàng không được để trống")
    @Size(max = 100, message = "Tên ngành hàng tối đa 100 ký tự")
    private String name;

    @NotBlank(message = "Slug ngành hàng không được để trống")
    @Size(max = 100, message = "Slug ngành hàng tối đa 100 ký tự")
    private String slug;

    @DecimalMin(value = "0.0000", message = "Tỷ lệ hoa hồng phải >= 0")
    @DecimalMax(value = "1.0000", message = "Tỷ lệ hoa hồng phải <= 1 (100%)")
    private BigDecimal commissionRate;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;
}
