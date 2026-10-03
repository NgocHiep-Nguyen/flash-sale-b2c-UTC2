package com.b2c.flash_sale_b2c_UTC2.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

/**
 * Cấu trúc phân trang chuẩn hóa cho các API trả về danh sách dữ liệu.
 *
 * @param <T> Kiểu dữ liệu phần tử trong trang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    @Builder.Default
    private List<T> items = Collections.emptyList();

    private int pageNumber;

    private int pageSize;

    private long totalElements;

    private int totalPages;

    private boolean isFirst;

    private boolean isLast;

    private boolean hasNext;

    private boolean hasPrevious;

    /**
     * Chuyển đổi trực tiếp từ đối tượng Page của Spring Data sang PageResponse.
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return of(page, page.getContent());
    }

    /**
     * Chuyển đổi từ Page gốc kết hợp với danh sách DTO đã được biến đổi.
     */
    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return PageResponse.<T>builder()
                .items(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
