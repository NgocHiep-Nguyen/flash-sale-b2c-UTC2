package com.b2c.flash_sale_b2c_UTC2.order.mapper;

import com.b2c.flash_sale_b2c_UTC2.order.dto.OrderResponse;
import com.b2c.flash_sale_b2c_UTC2.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "buyer.id", target = "buyerId")
    @Mapping(source = "store.id", target = "storeId")
    @Mapping(source = "slot.id", target = "slotId")
    @Mapping(source = "voucher.id", target = "voucherId")
    OrderResponse toResponse(Order order);
}
