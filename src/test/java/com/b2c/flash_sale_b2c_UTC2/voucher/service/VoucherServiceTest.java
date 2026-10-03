package com.b2c.flash_sale_b2c_UTC2.voucher.service;

import com.b2c.flash_sale_b2c_UTC2.common.exception.BusinessException;
import com.b2c.flash_sale_b2c_UTC2.store.entity.Store;
import com.b2c.flash_sale_b2c_UTC2.store.repository.StoreRepository;
import com.b2c.flash_sale_b2c_UTC2.user.entity.User;
import com.b2c.flash_sale_b2c_UTC2.voucher.dto.ApplyVoucherRequest;
import com.b2c.flash_sale_b2c_UTC2.voucher.dto.CreateVoucherRequest;
import com.b2c.flash_sale_b2c_UTC2.voucher.dto.VoucherCalculationResponse;
import com.b2c.flash_sale_b2c_UTC2.voucher.dto.VoucherResponse;
import com.b2c.flash_sale_b2c_UTC2.voucher.entity.Voucher;
import com.b2c.flash_sale_b2c_UTC2.voucher.exception.VoucherErrorCode;
import com.b2c.flash_sale_b2c_UTC2.voucher.mapper.VoucherMapper;
import com.b2c.flash_sale_b2c_UTC2.voucher.repository.VoucherRepository;
import com.b2c.flash_sale_b2c_UTC2.voucher.repository.VoucherUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private VoucherUsageRepository voucherUsageRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private VoucherMapper voucherMapper;

    @InjectMocks
    private VoucherServiceImpl voucherService;

    private User sampleUser;
    private Store sampleStore;
    private Voucher sampleVoucher;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("user@example.com").build();
        sampleStore = Store.builder().id(10L).user(sampleUser).storeName("Shop UTC2").build();

        sampleVoucher = Voucher.builder()
                .id(100L)
                .code("UTC2DISCOUNT")
                .store(sampleStore)
                .discountType("FIXED_AMOUNT")
                .discountValue(new BigDecimal("50000.00"))
                .minOrderAmount(new BigDecimal("200000.00"))
                .totalQuantity(10)
                .usedQuantity(0)
                .userUsageLimit(1)
                .startTime(Instant.now().minus(1, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(7, ChronoUnit.DAYS))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Tạo voucher thành công với mã hợp lệ")
    void createVoucher_Success() {
        CreateVoucherRequest request = CreateVoucherRequest.builder()
                .code("UTC2DISCOUNT")
                .storeId(10L)
                .discountType("FIXED_AMOUNT")
                .discountValue(new BigDecimal("50000.00"))
                .minOrderAmount(new BigDecimal("200000.00"))
                .totalQuantity(10)
                .startTime(Instant.now().minus(1, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        when(voucherRepository.existsByCode("UTC2DISCOUNT")).thenReturn(false);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(sampleStore));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(sampleVoucher);
        when(voucherMapper.toResponse(sampleVoucher)).thenReturn(VoucherResponse.builder().id(100L).code("UTC2DISCOUNT").build());

        VoucherResponse response = voucherService.createVoucher(1L, request);

        assertNotNull(response);
        assertEquals("UTC2DISCOUNT", response.getCode());
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Tạo voucher ném lỗi khi mã đã tồn tại")
    void createVoucher_ShouldThrowException_WhenCodeExists() {
        CreateVoucherRequest request = CreateVoucherRequest.builder().code("UTC2DISCOUNT").build();
        when(voucherRepository.existsByCode("UTC2DISCOUNT")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> voucherService.createVoucher(1L, request));
        assertEquals(VoucherErrorCode.VOUCHER_CODE_EXISTS.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    @DisplayName("Áp dụng voucher giảm giá cố định thành công")
    void validateAndCalculate_FixedAmount_Success() {
        ApplyVoucherRequest request = ApplyVoucherRequest.builder()
                .code("UTC2DISCOUNT")
                .storeId(10L)
                .subtotalAmount(new BigDecimal("300000.00"))
                .build();

        when(voucherRepository.findByCode("UTC2DISCOUNT")).thenReturn(Optional.of(sampleVoucher));
        when(voucherUsageRepository.countByVoucherIdAndUserId(100L, 1L)).thenReturn(0L);

        VoucherCalculationResponse response = voucherService.validateAndCalculate(1L, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("50000.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("250000.00"), response.getFinalAmount());
    }

    @Test
    @DisplayName("Áp dụng voucher phần trăm áp dụng mức giảm tối đa maxDiscountAmount")
    void validateAndCalculate_PercentDiscount_WithMaxDiscount_Success() {
        Voucher percentVoucher = Voucher.builder()
                .id(101L)
                .code("SALE20")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("20.00")) // 20%
                .maxDiscountAmount(new BigDecimal("30000.00")) // Giảm tối đa 30k
                .totalQuantity(10)
                .usedQuantity(0)
                .userUsageLimit(1)
                .startTime(Instant.now().minus(1, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(7, ChronoUnit.DAYS))
                .status("ACTIVE")
                .build();

        ApplyVoucherRequest request = ApplyVoucherRequest.builder()
                .code("SALE20")
                .subtotalAmount(new BigDecimal("200000.00")) // 20% của 200k = 40k -> Giới hạn max 30k
                .build();

        when(voucherRepository.findByCode("SALE20")).thenReturn(Optional.of(percentVoucher));

        VoucherCalculationResponse response = voucherService.validateAndCalculate(1L, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("30000.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("170000.00"), response.getFinalAmount());
    }

    @Test
    @DisplayName("Áp dụng voucher ném lỗi khi đã hết hạn")
    void validateVoucher_ShouldThrowException_WhenExpired() {
        Voucher expiredVoucher = Voucher.builder()
                .id(102L)
                .code("EXPIRED")
                .startTime(Instant.now().minus(10, ChronoUnit.DAYS))
                .endTime(Instant.now().minus(1, ChronoUnit.DAYS))
                .status("ACTIVE")
                .build();

        when(voucherRepository.findByCode("EXPIRED")).thenReturn(Optional.of(expiredVoucher));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                voucherService.validateVoucherForOrder("EXPIRED", 1L, 10L, new BigDecimal("100000.00")));
        assertEquals(VoucherErrorCode.VOUCHER_EXPIRED.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    @DisplayName("Áp dụng voucher ném lỗi khi gian hàng không khớp (VOUCHER_STORE_MISMATCH)")
    void validateVoucher_ShouldThrowException_WhenStoreMismatch() {
        when(voucherRepository.findByCode("UTC2DISCOUNT")).thenReturn(Optional.of(sampleVoucher));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                voucherService.validateVoucherForOrder("UTC2DISCOUNT", 1L, 999L, new BigDecimal("300000.00"))); // storeId khác 10L
        assertEquals(VoucherErrorCode.VOUCHER_STORE_MISMATCH.getCode(), exception.getErrorCode().getCode());
    }
}
