package com.b2c.flash_sale_b2c_UTC2.product.mapper;

import com.b2c.flash_sale_b2c_UTC2.product.dto.CategoryResponse;
import com.b2c.flash_sale_b2c_UTC2.product.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
    java.util.List<CategoryResponse> toResponseList(java.util.List<Category> categories);
}
