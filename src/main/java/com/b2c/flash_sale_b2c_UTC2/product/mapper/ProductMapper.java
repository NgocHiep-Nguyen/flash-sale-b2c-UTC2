package com.b2c.flash_sale_b2c_UTC2.product.mapper;

import com.b2c.flash_sale_b2c_UTC2.product.dto.ProductResponse;
import com.b2c.flash_sale_b2c_UTC2.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "store.id", target = "storeId")
    @Mapping(source = "category.id", target = "categoryId")
    ProductResponse toResponse(Product product);
}
