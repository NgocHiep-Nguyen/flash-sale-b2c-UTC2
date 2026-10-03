package com.b2c.flash_sale_b2c_UTC2.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoreRequest {

    @NotBlank(message = "Tên gian hàng không được để trống")
    @Size(min = 2, max = 150, message = "Tên gian hàng phải từ 2 đến 150 ký tự")
    private String storeName;

    @Size(max = 255, message = "URL logo không được vượt quá 255 ký tự")
    private String logoUrl;

    private String description;
}
