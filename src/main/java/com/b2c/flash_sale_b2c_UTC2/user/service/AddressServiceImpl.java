package com.b2c.flash_sale_b2c_UTC2.user.service;

import com.b2c.flash_sale_b2c_UTC2.common.exception.BusinessException;
import com.b2c.flash_sale_b2c_UTC2.user.dto.AddressResponse;
import com.b2c.flash_sale_b2c_UTC2.user.dto.CreateAddressRequest;
import com.b2c.flash_sale_b2c_UTC2.user.dto.UpdateAddressRequest;
import com.b2c.flash_sale_b2c_UTC2.user.entity.Address;
import com.b2c.flash_sale_b2c_UTC2.user.entity.User;
import com.b2c.flash_sale_b2c_UTC2.user.exception.UserErrorCode;
import com.b2c.flash_sale_b2c_UTC2.user.mapper.AddressMapper;
import com.b2c.flash_sale_b2c_UTC2.user.repository.AddressRepository;
import com.b2c.flash_sale_b2c_UTC2.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        // Sắp xếp địa chỉ mặc định lên đầu, sau đó theo ID giảm dần
        addresses.sort(Comparator.comparing(Address::getIsDefault, Comparator.reverseOrder())
                .thenComparing(Address::getId, Comparator.reverseOrder()));
        return addressMapper.toResponseList(addresses);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Long userId, Long addressId) {
        Address address = findAndValidateOwnership(userId, addressId);
        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, CreateAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        List<Address> currentAddresses = addressRepository.findByUserId(userId);
        boolean isFirst = currentAddresses.isEmpty();
        boolean shouldBeDefault = isFirst || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault) {
            for (Address addr : currentAddresses) {
                if (Boolean.TRUE.equals(addr.getIsDefault())) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            }
        }

        Address address = Address.builder()
                .user(user)
                .store(null) // Ràng buộc XOR CHECK: Địa chỉ của User thì store_id phải NULL
                .contactName(request.getContactName().trim())
                .phone(request.getPhone().trim())
                .province(request.getProvince().trim())
                .district(request.getDistrict().trim())
                .ward(request.getWard().trim())
                .detailAddress(request.getDetailAddress().trim())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isDefault(shouldBeDefault)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        address = addressRepository.save(address);
        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, UpdateAddressRequest request) {
        Address address = findAndValidateOwnership(userId, addressId);

        if (request.getIsDefault() != null && request.getIsDefault() && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(currentDefault -> {
                currentDefault.setIsDefault(false);
                addressRepository.save(currentDefault);
            });
            address.setIsDefault(true);
        }

        if (request.getContactName() != null && !request.getContactName().isBlank()) {
            address.setContactName(request.getContactName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            address.setPhone(request.getPhone().trim());
        }
        if (request.getProvince() != null && !request.getProvince().isBlank()) {
            address.setProvince(request.getProvince().trim());
        }
        if (request.getDistrict() != null && !request.getDistrict().isBlank()) {
            address.setDistrict(request.getDistrict().trim());
        }
        if (request.getWard() != null && !request.getWard().isBlank()) {
            address.setWard(request.getWard().trim());
        }
        if (request.getDetailAddress() != null && !request.getDetailAddress().isBlank()) {
            address.setDetailAddress(request.getDetailAddress().trim());
        }
        if (request.getLatitude() != null) {
            address.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            address.setLongitude(request.getLongitude());
        }

        address.setUpdatedAt(Instant.now());
        address = addressRepository.save(address);
        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findAndValidateOwnership(userId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        addressRepository.delete(address);

        // Nếu địa chỉ vừa xóa là mặc định và còn các địa chỉ khác, tự động đặt địa chỉ còn lại đầu tiên làm mặc định
        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserId(userId);
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        Address address = findAndValidateOwnership(userId, addressId);

        if (!Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(currentDefault -> {
                currentDefault.setIsDefault(false);
                addressRepository.save(currentDefault);
            });
            address.setIsDefault(true);
            address.setUpdatedAt(Instant.now());
            address = addressRepository.save(address);
        }

        return addressMapper.toResponse(address);
    }

    private Address findAndValidateOwnership(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ADDRESS_NOT_FOUND));

        if (address.getUser() == null || !address.getUser().getId().equals(userId)) {
            log.warn("Cảnh báo truy cập trái phép: User {} cố ý truy cập Address {}", userId, addressId);
            throw new BusinessException(UserErrorCode.ADDRESS_ACCESS_DENIED);
        }

        return address;
    }
}
