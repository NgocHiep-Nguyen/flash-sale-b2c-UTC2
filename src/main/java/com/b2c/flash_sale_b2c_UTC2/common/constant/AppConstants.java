package com.b2c.flash_sale_b2c_UTC2.common.constant;

/**
 * Các hằng số dùng chung toàn hệ thống trong giai đoạn skeleton.
 */
public final class AppConstants {

    private AppConstants() {
        // Private constructor để ngăn việc khởi tạo đối tượng utility class
    }

    // --- Pagination Constants ---
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final int MAX_PAGE_SIZE = 100;

    // --- Date & Time Format ---
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String DEFAULT_TIMEZONE = "UTC";

    // --- Security & HTTP Headers ---
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";

    // --- API Prefix ---
    public static final String API_V1_PREFIX = "/api/v1";
}
