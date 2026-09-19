package com.b2c.flash_sale_b2c_UTC2.common.exception;

import lombok.Getter;

/**
 * Exception nền tảng đại diện cho các lỗi nghiệp vụ trong hệ thống Flash Sale.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null && !customMessage.isBlank() ? customMessage : errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage != null && !customMessage.isBlank() ? customMessage : errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
