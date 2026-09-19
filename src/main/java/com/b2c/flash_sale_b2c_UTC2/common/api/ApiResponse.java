package com.b2c.flash_sale_b2c_UTC2.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Cấu trúc JSON response chuẩn hóa cho toàn bộ REST API của hệ thống.
 *
 * @param <T> Kiểu dữ liệu của payload trả về
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    private boolean success = true;

    private int code;

    private String message;

    private T data;

    @Builder.Default
    private Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Operation completed successfully")
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return error(status.value(), message);
    }
}
