package com.b2c.flash_sale_b2c_UTC2.flashsale.repository;

import com.b2c.flash_sale_b2c_UTC2.flashsale.entity.FlashSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, Long> {
    Optional<FlashSaleItem> findBySlotIdAndVariantId(Long slotId, Long variantId);
    List<FlashSaleItem> findBySlotIdAndStatus(Long slotId, String status);
    boolean existsByVariantIdIn(List<Long> variantIds);

    @Query("""
        SELECT count(fsi) > 0 FROM FlashSaleItem fsi
        WHERE fsi.variant.id = :variantId
        AND fsi.status = 'APPROVED'
        AND fsi.slot.status = 'ACTIVE'
    """)
    boolean isVariantInActiveFlashSale(@Param("variantId") Long variantId);

    @Query("""
        SELECT count(fsi) > 0 FROM FlashSaleItem fsi
        WHERE fsi.variant.id IN :variantIds
        AND fsi.status = 'APPROVED'
        AND fsi.slot.status = 'ACTIVE'
    """)
    boolean areAnyVariantsInActiveFlashSale(@Param("variantIds") List<Long> variantIds);
}
