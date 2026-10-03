package com.b2c.flash_sale_b2c_UTC2.cart.exception;

import com.b2c.flash_sale_b2c_UTC2.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CartErrorCode implements ErrorCode {
    CART_NOT_FOUND("CART_404", "Không tìm thấy giỏ hàng của người dùng", HttpStatus.NOT_FOUND),
    CART_ITEM_NOT_FOUND("CART_ITEM_404", "Không tìm thấy sản phẩm trong giỏ hàng", HttpStatus.NOT_FOUND),
    CART_ITEM_ACCESS_DENIED("CART_403", "Bạn không có quyền thao tác trên mục giỏ hàng này", HttpStatus.FORBIDDEN),
    INSUFFICIENT_STOCK("CART_400_1", "Số lượng tồn kho sản phẩm không đủ", HttpStatus.BAD_REQUEST),
    INVALID_QUANTITY("CART_400_2", "Số lượng sản phẩm thêm vào giỏ phải lớn hơn 0", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
