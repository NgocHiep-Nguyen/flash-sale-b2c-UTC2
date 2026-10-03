package com.b2c.flash_sale_b2c_UTC2.flashsale.service;

import com.b2c.flash_sale_b2c_UTC2.common.exception.BusinessException;
import com.b2c.flash_sale_b2c_UTC2.flashsale.dto.CreateReservationRequest;
import com.b2c.flash_sale_b2c_UTC2.flashsale.dto.ReservationResponse;
import com.b2c.flash_sale_b2c_UTC2.flashsale.entity.FlashSaleItem;
import com.b2c.flash_sale_b2c_UTC2.flashsale.entity.FlashSaleSlot;
import com.b2c.flash_sale_b2c_UTC2.flashsale.exception.FlashSaleErrorCode;
import com.b2c.flash_sale_b2c_UTC2.flashsale.port.CreateFlashSaleOrderCommand;
import com.b2c.flash_sale_b2c_UTC2.flashsale.port.FlashSaleOrderPort;
import com.b2c.flash_sale_b2c_UTC2.flashsale.port.OrderRef;
import com.b2c.flash_sale_b2c_UTC2.flashsale.repository.FlashSaleItemRepository;
import com.b2c.flash_sale_b2c_UTC2.flashsale.strategy.StockReservationStrategy;
import com.b2c.flash_sale_b2c_UTC2.user.entity.Address;
import com.b2c.flash_sale_b2c_UTC2.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleReservationService {

    private final FlashSaleItemRepository itemRepository;
    private final AddressRepository addressRepository;
    private final StockReservationStrategy stockReservationStrategy;

    @Autowired(required = false)
    private FlashSaleOrderPort orderPort;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:reservation:";

    @Transactional
    public ReservationResponse createReservation(Long userId, String idempotencyKey, CreateReservationRequest request) {
        if (idempotencyKey != null && !idempotencyKey.isBlank() && redisTemplate != null) {
            String idemKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            Boolean isFirstReq = redisTemplate.opsForValue().setIfAbsent(idemKey, "PROCESSING", Duration.ofMinutes(10));
            if (Boolean.FALSE.equals(isFirstReq)) {
                String existingVal = redisTemplate.opsForValue().get(idemKey);
                if ("PROCESSING".equals(existingVal)) {
                    log.warn("Duplicate reservation request in flight for idempotencyKey: {}", idempotencyKey);
                    throw new BusinessException(FlashSaleErrorCode.ORDER_CREATION_FAILED);
                }
            }
        }

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new BusinessException(FlashSaleErrorCode.ADDRESS_NOT_FOUND_OR_NOT_OWNED));

        if (address.getUser() == null || !address.getUser().getId().equals(userId)) {
            throw new BusinessException(FlashSaleErrorCode.ADDRESS_NOT_FOUND_OR_NOT_OWNED);
        }

        FlashSaleItem item = itemRepository.findById(request.getFlashSaleItemId())
                .orElseThrow(() -> new BusinessException(FlashSaleErrorCode.ITEM_NOT_FOUND));

        if (!"APPROVED".equals(item.getStatus())) {
            throw new BusinessException(FlashSaleErrorCode.ITEM_NOT_FOUND);
        }

        FlashSaleSlot slot = item.getSlot();
        Instant now = Instant.now();
        if (!"ACTIVE".equals(slot.getStatus()) && !(now.isAfter(slot.getStartTime()) && now.isBefore(slot.getEndTime()))) {
            throw new BusinessException(FlashSaleErrorCode.SLOT_NOT_ACTIVE);
        }

        long remainingSlotSeconds = Duration.between(now, slot.getEndTime()).getSeconds();
        if (remainingSlotSeconds <= 0) {
            throw new BusinessException(FlashSaleErrorCode.SLOT_ALREADY_ENDED);
        }

        long reservationResult = stockReservationStrategy.reserveStock(
                slot.getId(),
                userId,
                item.getId(),
                request.getQuantity(),
                item.getUserPurchaseLimit(),
                remainingSlotSeconds
        );

        if (reservationResult == -1) {
            throw new BusinessException(FlashSaleErrorCode.PURCHASE_LIMIT_EXCEEDED);
        }
        if (reservationResult == 0) {
            throw new BusinessException(FlashSaleErrorCode.OUT_OF_STOCK);
        }

        try {
            int dbUpdated = itemRepository.deductAvailableStockConditionally(item.getId(), request.getQuantity());
            if (dbUpdated == 0) {
                log.warn("DB available_stock was insufficient for FlashSaleItem ID: {}. Triggering compensation.", item.getId());
                stockReservationStrategy.compensate(slot.getId(), userId, item.getId(), request.getQuantity());
                throw new BusinessException(FlashSaleErrorCode.OUT_OF_STOCK);
            }

            if (orderPort == null) {
                log.error("FlashSaleOrderPort is not configured!");
                stockReservationStrategy.compensate(slot.getId(), userId, item.getId(), request.getQuantity());
                itemRepository.replenishAvailableStockConditionally(item.getId(), request.getQuantity());
                throw new BusinessException(FlashSaleErrorCode.ORDER_CREATION_FAILED);
            }

            Instant expiresAt = Instant.now().plusSeconds(slot.getReservationTtlSeconds());
            String orderCode = "FS-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 9000 + 1000);
            BigDecimal totalAmount = item.getFlashSalePrice().multiply(BigDecimal.valueOf(request.getQuantity()));

            CreateFlashSaleOrderCommand cmd = new CreateFlashSaleOrderCommand(
                    userId,
                    address.getId(),
                    item.getId(),
                    item.getVariant().getId(),
                    item.getVariant().getProduct().getStore().getId(),
                    request.getQuantity(),
                    item.getFlashSalePrice(),
                    item.getVariant().getProduct().getName(),
                    item.getVariant().getVariantName(),
                    orderCode,
                    expiresAt
            );

            OrderRef orderRef = orderPort.createPendingOrder(cmd);

            if (idempotencyKey != null && !idempotencyKey.isBlank() && redisTemplate != null) {
                redisTemplate.opsForValue().set(
                        IDEMPOTENCY_KEY_PREFIX + idempotencyKey,
                        orderRef.orderCode(),
                        Duration.ofMinutes(10)
                );
            }

            return ReservationResponse.builder()
                    .orderId(orderRef.orderId())
                    .orderCode(orderRef.orderCode())
                    .flashSaleItemId(item.getId())
                    .quantity(request.getQuantity())
                    .totalAmount(totalAmount)
                    .status("PENDING_PAYMENT")
                    .expiresAt(expiresAt)
                    .message("Đặt hàng giữ chỗ Flash Sale thành công. Vui lòng thanh toán trước thời hạn.")
                    .build();

        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.error("Error creating order down the line. Executing dual-write compensation for item ID: {}", item.getId(), ex);
            try {
                stockReservationStrategy.compensate(slot.getId(), userId, item.getId(), request.getQuantity());
                itemRepository.replenishAvailableStockConditionally(item.getId(), request.getQuantity());
            } catch (Exception compEx) {
                log.error("CRITICAL: Failed to compensate dual-write failure for item ID: {}", item.getId(), compEx);
            }
            throw new BusinessException(FlashSaleErrorCode.ORDER_CREATION_FAILED);
        }
    }
}
