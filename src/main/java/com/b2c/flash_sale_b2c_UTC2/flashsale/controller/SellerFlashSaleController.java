package com.b2c.flash_sale_b2c_UTC2.flashsale.controller;

import com.b2c.flash_sale_b2c_UTC2.auth.security.CustomUserDetails;
import com.b2c.flash_sale_b2c_UTC2.common.api.ApiResponse;
import com.b2c.flash_sale_b2c_UTC2.flashsale.dto.FlashSaleItemResponse;
import com.b2c.flash_sale_b2c_UTC2.flashsale.dto.RegisterFlashSaleItemRequest;
import com.b2c.flash_sale_b2c_UTC2.flashsale.service.FlashSaleItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/flash-sales")
@RequiredArgsConstructor
@Tag(name = "Seller Flash Sale", description = "APIs dành cho Người bán đăng ký sản phẩm tham gia Flash Sale")
@PreAuthorize("hasRole('SELLER')")
public class SellerFlashSaleController {

    private final FlashSaleItemService itemService;

    @PostMapping("/items")
    @Operation(summary = "Đăng ký biến thể tham gia Flash Sale", description = "Seller nộp đơn đăng ký biến thể SKU vào một khung giờ Flash Sale")
    public ResponseEntity<ApiResponse<FlashSaleItemResponse>> registerItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RegisterFlashSaleItemRequest request
    ) {
        FlashSaleItemResponse response = itemService.registerItem(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
