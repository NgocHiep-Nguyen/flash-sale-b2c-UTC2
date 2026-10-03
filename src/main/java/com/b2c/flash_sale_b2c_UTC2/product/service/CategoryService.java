package com.b2c.flash_sale_b2c_UTC2.product.service;

import com.b2c.flash_sale_b2c_UTC2.product.dto.CategoryResponse;
import com.b2c.flash_sale_b2c_UTC2.product.dto.CreateCategoryRequest;
import com.b2c.flash_sale_b2c_UTC2.product.dto.UpdateCategoryRequest;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Integer id);

    CategoryResponse getCategoryBySlug(String slug);

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(Integer id, UpdateCategoryRequest request);

    void deleteCategory(Integer id);
}
