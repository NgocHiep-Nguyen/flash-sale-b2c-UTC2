package com.b2c.flash_sale_b2c_UTC2.flashsale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFlashSaleSlotRequest {

    @NotBlank(message = "Tiêu đề phiên Flash Sale không được để trống")
    private String title;

    @NotNull(message = "Thời điểm bắt đầu không được để trống")
    private Instant startTime;

    @NotNull(message = "Thời điểm kết thúc không được để trống")
    private Instant endTime;

    @Positive(message = "Thời gian giữ chỗ phải lớn hơn 0")
    private Integer reservationTtlSeconds;

    private String status;
}
