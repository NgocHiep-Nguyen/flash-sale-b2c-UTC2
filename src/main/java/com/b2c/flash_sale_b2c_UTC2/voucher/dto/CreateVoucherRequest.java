package com.b2c.flash_sale_b2c_UTC2.voucher.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
public class CreateVoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 30, message = "Mã voucher tối đa 30 ký tự")
    private String code;

    private Long storeId; // null = Platform voucher, non-null = Store voucher

    @NotBlank(message = "Loại giảm giá không được để trống (FIXED_AMOUNT hoặc PERCENT)")
    private String discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @Positive(message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;

    @PositiveOrZero(message = "Giá trị đơn hàng tối thiểu không được âm")
    private BigDecimal minOrderAmount;

    @Positive(message = "Giá trị giảm tối đa phải lớn hơn 0")
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Tổng số lượng voucher không được để trống")
    @Positive(message = "Tổng số lượng voucher phải lớn hơn 0")
    private Integer totalQuantity;

    @Positive(message = "Giới hạn sử dụng mỗi user phải lớn hơn 0")
    private Integer userUsageLimit;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private Instant startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @Future(message = "Thời gian kết thúc phải ở tương lai")
    private Instant endTime;
}
