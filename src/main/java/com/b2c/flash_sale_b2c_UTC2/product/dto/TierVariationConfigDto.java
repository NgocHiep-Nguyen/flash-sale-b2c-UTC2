package com.b2c.flash_sale_b2c_UTC2.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierVariationConfigDto implements Serializable {

    @NotBlank(message = "Tên tầng phân loại không được để trống (ví dụ: Màu sắc, Kích cỡ)")
    private String name;

    @NotEmpty(message = "Danh sách tùy chọn không được để trống (ví dụ: [\"Đen\", \"Trắng\"])")
    private List<String> options;
}
