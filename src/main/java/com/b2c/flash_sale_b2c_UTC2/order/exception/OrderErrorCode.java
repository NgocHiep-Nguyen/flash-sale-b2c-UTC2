package com.b2c.flash_sale_b2c_UTC2.order.exception;

import com.b2c.flash_sale_b2c_UTC2.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER_404", "Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND),
    ORDER_ACCESS_DENIED("ORDER_403", "Bạn không có quyền truy cập hoặc thao tác trên đơn hàng này", HttpStatus.FORBIDDEN),
    EMPTY_ORDER_ITEMS("ORDER_400_1", "Đơn hàng phải chứa ít nhất một sản phẩm", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_VARIANT_STOCK("ORDER_400_2", "Số lượng tồn kho sản phẩm không đủ để đặt hàng", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_TRANSITION("ORDER_400_3", "Trạng thái chuyển đổi của đơn hàng không hợp lệ", HttpStatus.BAD_REQUEST),
    STORE_NOT_APPROVED("ORDER_400_4", "Gian hàng chưa được duyệt hoặc đang bị khóa, không thể đặt hàng", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
