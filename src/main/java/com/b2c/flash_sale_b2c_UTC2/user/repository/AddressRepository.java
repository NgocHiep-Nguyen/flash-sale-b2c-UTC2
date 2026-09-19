package com.b2c.flash_sale_b2c_UTC2.user.repository;

import com.b2c.flash_sale_b2c_UTC2.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);
    List<Address> findByStoreId(Long storeId);
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
    Optional<Address> findByStoreIdAndIsDefaultTrue(Long storeId);
}
