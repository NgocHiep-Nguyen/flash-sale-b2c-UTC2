package com.b2c.flash_sale_b2c_UTC2.voucher.mapper;

import com.b2c.flash_sale_b2c_UTC2.voucher.dto.VoucherResponse;
import com.b2c.flash_sale_b2c_UTC2.voucher.entity.Voucher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VoucherMapper {

    @Mapping(source = "store.id", target = "storeId")
    VoucherResponse toResponse(Voucher voucher);
}
