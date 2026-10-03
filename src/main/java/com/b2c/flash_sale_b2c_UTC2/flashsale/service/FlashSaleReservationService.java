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
        String idemKey = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank() && redisTemplate != null) {
            idemKey = IDEMPOTENCY_KEY_PREFIX + userId + ":" + idempotencyKey;
            Boolean isFirstReq = redisTemplate.opsForValue().setIfAbsent(idemKey, "PROCESSING", Duration.ofMinutes(10));
            if (Boolean.FALSE.equals(isFirstReq)) {
                String existingVal = redisTemplate.opsForValue().get(idemKey);
                if ("PROCESSING".equals(existingVal)) {
                    log.warn("Duplicate reservation request in flight for userId: {}, idempotencyKey: {}", userId, idempotencyKey);
                    throw new BusinessException(FlashSaleErrorCode.IDEMPOTENCY_CONFLICT);
                }
                if (existingVal != null && !existingVal.isBlank()) {
                    log.info("Returning cached order result for idempotencyKey: {} -> {}", idempotencyKey, existingVal);
                    return ReservationResponse.builder()
                            .orderCode(existingVal)
                            .status("PENDING_PAYMENT")
                            .message("Yêu cầu đã được xử lý thành công trước đó (Idempotent replay).")
                            .build();
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

        // Strict time check: reserve after end_time or before start_time must be rejected
        if (now.isBefore(slot.getStartTime()) || now.isAfter(slot.getEndTime()) || !"ACTIVE".equals(slot.getStatus())) {
            throw new BusinessException(FlashSaleErrorCode.SLOT_NOT_ACTIVE);
        }

        long remainingSlotSeconds = Duration.between(now, slot.getEndTime()).getSeconds();
        if (remainingSlotSeconds <= 0) {
            throw new BusinessException(FlashSaleErrorCode.SLOT_ALREADY_ENDED);
        }

        // Tracking flags for precise compensation
        boolean redisDeducted = false;
        boolean dbDeducted = false;

        long reservationResult = stockReservationStrategy.reserveStock(
                slot.getId(),
                userId,
                item.getId(),
                request.getQuantity(),
                item.getUserPurchaseLimit(),
                remainingSlotSeconds
        );

        if (reservationResult == -1) {
            if (idemKey != null) redisTemplate.delete(idemKey);
            throw new BusinessException(FlashSaleErrorCode.PURCHASE_LIMIT_EXCEEDED);
        }
        if (reservationResult == 0) {
            if (idemKey != null) redisTemplate.delete(idemKey);
            throw new BusinessException(FlashSaleErrorCode.OUT_OF_STOCK);
        }

        redisDeducted = true;

        try {
            int dbUpdated = itemRepository.deductAvailableStockConditionally(item.getId(), request.getQuantity());
            if (dbUpdated == 0) {
                log.warn("DB available_stock was insufficient for FlashSaleItem ID: {}. Triggering Redis compensation.", item.getId());
                stockReservationStrategy.compensate(slot.getId(), userId, item.getId(), request.getQuantity());
                redisDeducted = false;
                if (idemKey != null) redisTemplate.delete(idemKey);
                throw new BusinessException(FlashSaleErrorCode.OUT_OF_STOCK);
            }

            dbDeducted = true;

            if (orderPort == null) {
                log.error("FlashSaleOrderPort is not configured!");
                throw new BusinessException(FlashSaleErrorCode.ORDER_CREATION_FAILED);
            }

            Instant expiresAt = Instant.now().plusSeconds(slot.getReservationTtlSeconds());
            String orderCode = "FS-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 9000 + 1000);
            BigDecimal totalAmount = item.getFlashSalePrice().multiply(BigDecimal.valueOf(request.getQuantity()));

            CreateFlashSaleOrderCommand cmd = new CreateFlashSaleOrderCommand(
                    userId,
                    address.getId(),
                    item.getId(),
                    slot.getId(),
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

            if (idemKey != null && redisTemplate != null) {
                redisTemplate.opsForValue().set(idemKey, orderRef.orderCode(), Duration.ofMinutes(10));
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
            // Re-throw known BusinessException
            throw be;
        } catch (Exception ex) {
            log.error("Error creating order down the line. Executing precise compensation. redisDeducted={}, dbDeducted={}",
                    redisDeducted, dbDeducted, ex);
            try {
                if (redisDeducted) {
                    stockReservationStrategy.compensate(slot.getId(), userId, item.getId(), request.getQuantity());
                }
                if (dbDeducted) {
                    itemRepository.replenishAvailableStockConditionally(item.getId(), request.getQuantity());
                }
                if (idemKey != null && redisTemplate != null) {
                    redisTemplate.delete(idemKey);
                }
            } catch (Exception compEx) {
                log.error("CRITICAL: Failed during precise compensation for item ID: {}", item.getId(), compEx);
            }
            throw new BusinessException(FlashSaleErrorCode.ORDER_CREATION_FAILED);
        }
    }
}
