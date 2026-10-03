package com.b2c.flash_sale_b2c_UTC2.flashsale.service;

import com.b2c.flash_sale_b2c_UTC2.common.exception.BusinessException;
import com.b2c.flash_sale_b2c_UTC2.flashsale.dto.CreateFlashSaleSlotRequest;
import com.b2c.flash_sale_b2c_UTC2.flashsale.dto.FlashSaleSlotResponse;
import com.b2c.flash_sale_b2c_UTC2.flashsale.dto.UpdateFlashSaleSlotRequest;
import com.b2c.flash_sale_b2c_UTC2.flashsale.entity.FlashSaleSlot;
import com.b2c.flash_sale_b2c_UTC2.flashsale.exception.FlashSaleErrorCode;
import com.b2c.flash_sale_b2c_UTC2.flashsale.mapper.FlashSaleSlotMapper;
import com.b2c.flash_sale_b2c_UTC2.flashsale.repository.FlashSaleSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleSlotService {

    private final FlashSaleSlotRepository slotRepository;
    private final FlashSaleSlotMapper slotMapper;

    @Transactional
    public FlashSaleSlotResponse createSlot(CreateFlashSaleSlotRequest request) {
        validateSlotTimes(request.getStartTime(), request.getEndTime());

        boolean hasOverlap = slotRepository.existsOverlappingSlot(null, request.getStartTime(), request.getEndTime());
        if (hasOverlap) {
            throw new BusinessException(FlashSaleErrorCode.SLOT_OVERLAP);
        }

        FlashSaleSlot slot = FlashSaleSlot.builder()
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reservationTtlSeconds(request.getReservationTtlSeconds() != null ? request.getReservationTtlSeconds() : 300)
                .status("UPCOMING")
                .createdAt(Instant.now())
                .build();

        return slotMapper.toResponse(slotRepository.save(slot));
    }

    @Transactional
    public FlashSaleSlotResponse updateSlot(Long slotId, UpdateFlashSaleSlotRequest request) {
        FlashSaleSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(FlashSaleErrorCode.SLOT_NOT_FOUND));

        if ("ENDED".equals(slot.getStatus())) {
            throw new BusinessException(FlashSaleErrorCode.SLOT_ALREADY_ENDED);
        }

        validateSlotTimes(request.getStartTime(), request.getEndTime());

        boolean hasOverlap = slotRepository.existsOverlappingSlot(slotId, request.getStartTime(), request.getEndTime());
        if (hasOverlap) {
            throw new BusinessException(FlashSaleErrorCode.SLOT_OVERLAP);
        }

        slot.setTitle(request.getTitle());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        if (request.getReservationTtlSeconds() != null) {
            slot.setReservationTtlSeconds(request.getReservationTtlSeconds());
        }
        if (request.getStatus() != null) {
            slot.setStatus(request.getStatus());
        }

        return slotMapper.toResponse(slotRepository.save(slot));
    }

    @Transactional(readOnly = true)
    public FlashSaleSlotResponse getSlotById(Long slotId) {
        FlashSaleSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(FlashSaleErrorCode.SLOT_NOT_FOUND));
        return slotMapper.toResponse(slot);
    }

    @Transactional(readOnly = true)
    public List<FlashSaleSlotResponse> getAllSlots() {
        return slotRepository.findAll().stream()
                .map(slotMapper::toResponse)
                .toList();
    }

    private void validateSlotTimes(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BusinessException(FlashSaleErrorCode.SLOT_TIME_INVALID);
        }
    }
}
