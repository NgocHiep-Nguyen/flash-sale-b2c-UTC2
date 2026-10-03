package com.b2c.flash_sale_b2c_UTC2.user.service;

import com.b2c.flash_sale_b2c_UTC2.user.dto.AddressResponse;
import com.b2c.flash_sale_b2c_UTC2.user.dto.CreateAddressRequest;
import com.b2c.flash_sale_b2c_UTC2.user.dto.UpdateAddressRequest;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getUserAddresses(Long userId);

    AddressResponse getAddressById(Long userId, Long addressId);

    AddressResponse createAddress(Long userId, CreateAddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, UpdateAddressRequest request);

    void deleteAddress(Long userId, Long addressId);

    AddressResponse setDefaultAddress(Long userId, Long addressId);
}
