package com.b2c.flash_sale_b2c_UTC2.flashsale.mapper;

import com.b2c.flash_sale_b2c_UTC2.flashsale.dto.FlashSaleItemResponse;
import com.b2c.flash_sale_b2c_UTC2.flashsale.entity.FlashSaleItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FlashSaleItemMapper {

    @Mapping(source = "slot.id", target = "slotId")
    @Mapping(source = "variant.id", target = "variantId")
    FlashSaleItemResponse toResponse(FlashSaleItem item);
}
