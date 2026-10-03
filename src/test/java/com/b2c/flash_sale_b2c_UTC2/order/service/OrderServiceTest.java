package com.b2c.flash_sale_b2c_UTC2.order.service;

import com.b2c.flash_sale_b2c_UTC2.cart.repository.CartItemRepository;
import com.b2c.flash_sale_b2c_UTC2.cart.repository.CartRepository;
import com.b2c.flash_sale_b2c_UTC2.common.exception.BusinessException;
import com.b2c.flash_sale_b2c_UTC2.order.dto.CheckoutItemRequest;
import com.b2c.flash_sale_b2c_UTC2.order.dto.CheckoutStoreOrderRequest;
import com.b2c.flash_sale_b2c_UTC2.order.dto.CreateOrderRequest;
import com.b2c.flash_sale_b2c_UTC2.order.dto.OrderResponse;
import com.b2c.flash_sale_b2c_UTC2.order.dto.UpdateOrderStatusRequest;
import com.b2c.flash_sale_b2c_UTC2.order.entity.Order;
import com.b2c.flash_sale_b2c_UTC2.order.entity.OrderItem;
import com.b2c.flash_sale_b2c_UTC2.order.mapper.OrderMapper;
import com.b2c.flash_sale_b2c_UTC2.order.repository.OrderItemRepository;
import com.b2c.flash_sale_b2c_UTC2.order.repository.OrderRepository;
import com.b2c.flash_sale_b2c_UTC2.product.entity.Product;
import com.b2c.flash_sale_b2c_UTC2.product.entity.ProductVariant;
import com.b2c.flash_sale_b2c_UTC2.product.repository.ProductVariantRepository;
import com.b2c.flash_sale_b2c_UTC2.store.entity.Store;
import com.b2c.flash_sale_b2c_UTC2.store.repository.StoreRepository;
import com.b2c.flash_sale_b2c_UTC2.user.entity.Address;
import com.b2c.flash_sale_b2c_UTC2.user.entity.User;
import com.b2c.flash_sale_b2c_UTC2.user.exception.UserErrorCode;
import com.b2c.flash_sale_b2c_UTC2.user.repository.AddressRepository;
import com.b2c.flash_sale_b2c_UTC2.user.repository.UserRepository;
import com.b2c.flash_sale_b2c_UTC2.voucher.repository.VoucherRepository;
import com.b2c.flash_sale_b2c_UTC2.voucher.repository.VoucherUsageRepository;
import com.b2c.flash_sale_b2c_UTC2.voucher.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private VoucherUsageRepository voucherUsageRepository;

    @Mock
    private VoucherService voucherService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User buyer;
    private Store store;
    private Address address;
    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        buyer = User.builder().id(1L).email("buyer@example.com").build();
        store = Store.builder().id(10L).storeName("Shop UTC2").status("APPROVED").defaultCommissionRate(new BigDecimal("0.0500")).build();

        address = Address.builder()
                .id(100L)
                .user(buyer)
                .contactName("Nguyễn Văn A")
                .phone("0987654321")
                .province("TP.HCM")
                .district("Quận 9")
                .ward("Tăng Nhơn Phú A")
                .detailAddress("123 Lê Văn Việt")
                .build();

        product = Product.builder().id(50L).name("Laptop UTC2").store(store).build();
        variant = ProductVariant.builder()
                .id(500L)
                .product(product)
                .variantName("i7 / 16GB")
                .originalPrice(new BigDecimal("20000000.00"))
                .stockQuantity(10)
                .build();
    }

    @Test
    @DisplayName("Đặt hàng thành công với địa chỉ chính chủ và trừ tồn kho gốc")
    void createOrders_Success() {
        CheckoutItemRequest itemReq = CheckoutItemRequest.builder().variantId(500L).quantity(1).build();
        CheckoutStoreOrderRequest storeReq = CheckoutStoreOrderRequest.builder().storeId(10L).items(List.of(itemReq)).build();
        CreateOrderRequest request = CreateOrderRequest.builder().shippingAddressId(100L).storeOrders(List.of(storeReq)).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(addressRepository.findById(100L)).thenReturn(Optional.of(address));
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(productVariantRepository.findById(500L)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        Order savedOrder = Order.builder().id(1000L).orderCode("ORD-12345").buyer(buyer).store(store).subtotalAmount(new BigDecimal("20000000.00")).totalAmount(new BigDecimal("20000000.00")).build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().id(1000L).orderCode("ORD-12345").build());

        List<OrderResponse> responses = orderService.createOrders(1L, request);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(9, variant.getStockQuantity()); // Tồn kho giảm từ 10 -> 9
        verify(productVariantRepository).save(variant);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Đặt hàng thất bại khi địa chỉ không thuộc quyền sở hữu của Buyer (403)")
    void createOrders_ShouldThrowException_WhenAddressNotOwned() {
        User anotherUser = User.builder().id(999L).build();
        Address anotherAddress = Address.builder().id(200L).user(anotherUser).build();

        CreateOrderRequest request = CreateOrderRequest.builder().shippingAddressId(200L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(addressRepository.findById(200L)).thenReturn(Optional.of(anotherAddress));

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.createOrders(1L, request));
        assertEquals(UserErrorCode.ADDRESS_ACCESS_DENIED.getCode(), exception.getErrorCode().getCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hủy đơn hàng chưa thanh toán hoàn trả lại số kho tương ứng")
    void cancelOrder_Success_ShouldRestoreStock() {
        Order pendingOrder = Order.builder().id(1000L).buyer(buyer).store(store).status("PENDING_PAYMENT").build();
        OrderItem item = OrderItem.builder().id(5000L).order(pendingOrder).variant(variant).quantity(2).build();

        when(orderRepository.findById(1000L)).thenReturn(Optional.of(pendingOrder));
        when(orderItemRepository.findByOrderId(1000L)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);
        when(orderMapper.toResponse(pendingOrder)).thenReturn(OrderResponse.builder().id(1000L).status("CANCELLED").build());

        OrderResponse response = orderService.cancelOrder(1L, 1000L);

        assertNotNull(response);
        assertEquals(12, variant.getStockQuantity()); // Tồn kho hoàn trả từ 10 -> 12
        verify(productVariantRepository).save(variant);
        assertEquals("CANCELLED", pendingOrder.getStatus());
    }

    @Test
    @DisplayName("Seller cập nhật trạng thái đơn hàng thành công")
    void updateOrderStatusBySeller_Success() {
        User sellerUser = User.builder().id(2L).build();
        Store sellerStore = Store.builder().id(10L).user(sellerUser).build();

        Order order = Order.builder().id(1000L).store(sellerStore).status("PAID").build();
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder().status("SHIPPED").build();

        when(storeRepository.findByUserId(2L)).thenReturn(Optional.of(sellerStore));
        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(1000L).status("SHIPPED").build());

        OrderResponse response = orderService.updateOrderStatusBySeller(2L, 1000L, request);

        assertNotNull(response);
        assertEquals("SHIPPED", order.getStatus());
        verify(orderRepository).save(order);
    }
}
