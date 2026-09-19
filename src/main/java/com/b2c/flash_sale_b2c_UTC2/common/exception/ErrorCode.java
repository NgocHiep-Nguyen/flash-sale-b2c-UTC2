package com.b2c.flash_sale_b2c_UTC2.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Interface chuẩn cho các mã lỗi (Error Codes) trong toàn bộ hệ thống.
 * Cho phép các module nghiệp vụ tự định nghĩa Enum mã lỗi riêng.
 */
public interface ErrorCode {

    HttpStatus getHttpStatus();

    String getCode();

    String getMessage();
}
