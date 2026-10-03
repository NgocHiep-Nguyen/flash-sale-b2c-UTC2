package com.b2c.flash_sale_b2c_UTC2.store.exception;

import com.b2c.flash_sale_b2c_UTC2.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StoreErrorCode implements ErrorCode {

    STORE_ALREADY_EXISTS(HttpStatus.CONFLICT, "STORE_409_EXISTS", "Bạn đã có gian hàng trên hệ thống, không thể đăng ký thêm"),
    STORE_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "STORE_409_NAME_EXISTS", "Tên gian hàng này đã được sử dụng"),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_404_NOT_FOUND", "Không tìm thấy thông tin gian hàng"),
    STORE_NOT_APPROVED(HttpStatus.BAD_REQUEST, "STORE_400_NOT_APPROVED", "Gian hàng chưa được phê duyệt hoặc đang bị tạm khóa"),
    STORE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "STORE_403_ACCESS_DENIED", "Bạn không có quyền quản lý gian hàng này"),
    INVALID_STORE_STATUS(HttpStatus.BAD_REQUEST, "STORE_400_INVALID_STATUS", "Trạng thái phê duyệt gian hàng không hợp lệ"),
    STORE_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_404_ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ kho của gian hàng"),
    STORE_ADDRESS_ACCESS_DENIED(HttpStatus.FORBIDDEN, "STORE_403_ADDRESS_ACCESS_DENIED", "Địa chỉ kho không thuộc quyền sở hữu của gian hàng này");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
