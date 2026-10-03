package com.b2c.flash_sale_b2c_UTC2.voucher.controller;

import com.b2c.flash_sale_b2c_UTC2.auth.security.CustomUserDetails;
import com.b2c.flash_sale_b2c_UTC2.common.api.ApiResponse;
import com.b2c.flash_sale_b2c_UTC2.voucher.dto.CreateVoucherRequest;
import com.b2c.flash_sale_b2c_UTC2.voucher.dto.VoucherResponse;
import com.b2c.flash_sale_b2c_UTC2.voucher.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Seller Vouchers", description = "Quản lý mã giảm giá của gian hàng")
@RestController
@RequestMapping("/api/v1/seller/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerVoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "Tạo mã giảm giá mới cho gian hàng", description = "Seller tạo mã giảm giá riêng thuộc gian hàng mình sở hữu")
    @PostMapping
    public ResponseEntity<ApiResponse<VoucherResponse>> createStoreVoucher(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateVoucherRequest request
    ) {
        VoucherResponse response = voucherService.createVoucher(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mã giảm giá gian hàng thành công", response));
    }

    @Operation(summary = "Xem danh sách mã giảm giá của tôi", description = "Seller lấy danh sách mã giảm giá do gian hàng mình phát hành")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getMyStoreVouchers(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<VoucherResponse> response = voucherService.getMyStoreVouchers(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách mã giảm giá gian hàng thành công", response));
    }
}
