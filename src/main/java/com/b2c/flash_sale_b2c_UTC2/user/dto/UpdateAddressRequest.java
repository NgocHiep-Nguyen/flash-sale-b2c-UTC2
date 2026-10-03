package com.b2c.flash_sale_b2c_UTC2.user.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddressRequest {

    @Size(max = 100, message = "Tên người liên hệ không được vượt quá 100 ký tự")
    private String contactName;

    @Size(max = 15, message = "Số điện thoại không được vượt quá 15 ký tự")
    private String phone;

    @Size(max = 100, message = "Tỉnh/Thành phố không được vượt quá 100 ký tự")
    private String province;

    @Size(max = 100, message = "Quận/Huyện không được vượt quá 100 ký tự")
    private String district;

    @Size(max = 100, message = "Phường/Xã không được vượt quá 100 ký tự")
    private String ward;

    @Size(max = 255, message = "Địa chỉ chi tiết không được vượt quá 255 ký tự")
    private String detailAddress;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Boolean isDefault;
}
