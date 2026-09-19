package com.b2c.flash_sale_b2c_UTC2.common.util;

import com.b2c.flash_sale_b2c_UTC2.common.constant.AppConstants;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Tiện ích xử lý ngày giờ chuẩn UTC cho toàn hệ thống.
 */
public final class DateTimeUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter
            .ofPattern(AppConstants.DATE_TIME_FORMAT)
            .withZone(ZoneId.of(AppConstants.DEFAULT_TIMEZONE));

    private DateTimeUtils() {
        // Private constructor để ngăn tạo thể hiện
    }

    public static Instant nowUtc() {
        return Instant.now();
    }

    public static String formatIso(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ISO_FORMATTER.format(instant);
    }
}
