package com.b2c.flash_sale_b2c_UTC2.user.dto;

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
public class AddressResponse {

    private Long id;

    private String contactName;

    private String phone;

    private String province;

    private String district;

    private String ward;

    private String detailAddress;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Boolean isDefault;

    private Instant createdAt;

    private Instant updatedAt;
}
