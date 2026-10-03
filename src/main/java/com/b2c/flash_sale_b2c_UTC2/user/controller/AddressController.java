package com.b2c.flash_sale_b2c_UTC2.user.controller;

import com.b2c.flash_sale_b2c_UTC2.auth.security.CustomUserDetails;
import com.b2c.flash_sale_b2c_UTC2.common.api.ApiResponse;
import com.b2c.flash_sale_b2c_UTC2.common.exception.BusinessException;
import com.b2c.flash_sale_b2c_UTC2.common.exception.CommonErrorCode;
import com.b2c.flash_sale_b2c_UTC2.user.dto.AddressResponse;
import com.b2c.flash_sale_b2c_UTC2.user.dto.CreateAddressRequest;
import com.b2c.flash_sale_b2c_UTC2.user.dto.UpdateAddressRequest;
import com.b2c.flash_sale_b2c_UTC2.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Address Book", description = "Quản lý sổ địa chỉ giao hàng của người dùng")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/users/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "Danh sách sổ địa chỉ", description = "Lấy toàn bộ địa chỉ của người dùng hiện tại (địa chỉ mặc định ưu tiên xếp đầu)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getUserAddresses(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentUserId = validateAndGetUserId(userDetails);
        List<AddressResponse> responses = addressService.getUserAddresses(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "Thêm mới địa chỉ", description = "Tạo một địa chỉ mới trong sổ địa chỉ của người dùng")
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateAddressRequest request
    ) {
        Long currentUserId = validateAndGetUserId(userDetails);
        AddressResponse response = addressService.createAddress(currentUserId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm mới địa chỉ thành công", response));
    }

    @Operation(summary = "Xem chi tiết một địa chỉ", description = "Lấy thông tin chi tiết một địa chỉ theo ID (chỉ chủ sở hữu mới xem được)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id
    ) {
        Long currentUserId = validateAndGetUserId(userDetails);
        AddressResponse response = addressService.getAddressById(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Cập nhật địa chỉ", description = "Chỉnh sửa thông tin địa chỉ theo ID")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAddressRequest request
    ) {
        Long currentUserId = validateAndGetUserId(userDetails);
        AddressResponse response = addressService.updateAddress(currentUserId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa chỉ thành công", response));
    }

    @Operation(summary = "Xóa địa chỉ", description = "Xóa một địa chỉ khỏi sổ địa chỉ của người dùng")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id
    ) {
        Long currentUserId = validateAndGetUserId(userDetails);
        addressService.deleteAddress(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa địa chỉ thành công", null));
    }

    @Operation(summary = "Thiết lập địa chỉ mặc định", description = "Đặt địa chỉ được chọn làm địa chỉ mặc định duy nhất của người dùng")
    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id
    ) {
        Long currentUserId = validateAndGetUserId(userDetails);
        AddressResponse response = addressService.setDefaultAddress(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã thiết lập địa chỉ mặc định", response));
    }

    private Long validateAndGetUserId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return userDetails.getId();
    }
}
