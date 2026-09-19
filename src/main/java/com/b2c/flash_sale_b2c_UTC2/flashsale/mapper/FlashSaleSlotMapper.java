package com.b2c.flash_sale_b2c_UTC2.flashsale.mapper;

import com.b2c.flash_sale_b2c_UTC2.flashsale.dto.FlashSaleSlotResponse;
import com.b2c.flash_sale_b2c_UTC2.flashsale.entity.FlashSaleSlot;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FlashSaleSlotMapper {
    FlashSaleSlotResponse toResponse(FlashSaleSlot slot);
}
