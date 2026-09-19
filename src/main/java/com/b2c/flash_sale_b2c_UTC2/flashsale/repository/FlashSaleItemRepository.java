package com.b2c.flash_sale_b2c_UTC2.flashsale.repository;

import com.b2c.flash_sale_b2c_UTC2.flashsale.entity.FlashSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, Long> {
    Optional<FlashSaleItem> findBySlotIdAndVariantId(Long slotId, Long variantId);
    List<FlashSaleItem> findBySlotIdAndStatus(Long slotId, String status);
}
