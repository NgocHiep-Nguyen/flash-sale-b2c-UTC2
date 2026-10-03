package com.b2c.flash_sale_b2c_UTC2.cart.service;

import com.b2c.flash_sale_b2c_UTC2.cart.dto.AddToCartRequest;
import com.b2c.flash_sale_b2c_UTC2.cart.dto.CartResponse;
import com.b2c.flash_sale_b2c_UTC2.cart.dto.UpdateCartItemRequest;
import com.b2c.flash_sale_b2c_UTC2.cart.entity.Cart;
import com.b2c.flash_sale_b2c_UTC2.cart.entity.CartItem;
import com.b2c.flash_sale_b2c_UTC2.cart.exception.CartErrorCode;
import com.b2c.flash_sale_b2c_UTC2.cart.repository.CartItemRepository;
import com.b2c.flash_sale_b2c_UTC2.cart.repository.CartRepository;
import com.b2c.flash_sale_b2c_UTC2.common.exception.BusinessException;
import com.b2c.flash_sale_b2c_UTC2.product.entity.Product;
import com.b2c.flash_sale_b2c_UTC2.product.entity.ProductVariant;
import com.b2c.flash_sale_b2c_UTC2.product.repository.ProductVariantRepository;
import com.b2c.flash_sale_b2c_UTC2.store.entity.Store;
import com.b2c.flash_sale_b2c_UTC2.user.entity.User;
import com.b2c.flash_sale_b2c_UTC2.user.repository.UserRepository;
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
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User sampleUser;
    private Cart sampleCart;
    private Store sampleStore;
    private Product sampleProduct;
    private ProductVariant sampleVariant;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("buyer@example.com").build();
        sampleCart = Cart.builder().id(10L).user(sampleUser).build();
        sampleStore = Store.builder().id(100L).storeName("Shop UTC2").build();

        sampleProduct = Product.builder().id(50L).name("Áo thun UTC2").store(sampleStore).build();
        sampleVariant = ProductVariant.builder()
                .id(500L)
                .product(sampleProduct)
                .sku("SKU-AO-XL")
                .variantName("Size XL")
                .originalPrice(new BigDecimal("150000.00"))
                .stockQuantity(20)
                .build();
    }

    @Test
    @DisplayName("Thêm sản phẩm vào giỏ hàng thành công")
    void addToCart_Success() {
        AddToCartRequest request = AddToCartRequest.builder().variantId(500L).quantity(2).build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        when(productVariantRepository.findById(500L)).thenReturn(Optional.of(sampleVariant));
        when(cartItemRepository.findByCartIdAndVariantId(10L, 500L)).thenReturn(Optional.empty());

        CartItem savedItem = CartItem.builder().id(1000L).cart(sampleCart).variant(sampleVariant).quantity(2).build();
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedItem);
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(savedItem));

        CartResponse response = cartService.addToCart(1L, request);

        assertNotNull(response);
        assertEquals(1, response.getStoreGroups().size());
        assertEquals("Shop UTC2", response.getStoreGroups().get(0).getStoreName());
        assertEquals(new BigDecimal("300000.00"), response.getGrandTotal());
    }

    @Test
    @DisplayName("Thêm sản phẩm ném lỗi khi tồn kho không đủ")
    void addToCart_ShouldThrowException_WhenInsufficientStock() {
        AddToCartRequest request = AddToCartRequest.builder().variantId(500L).quantity(50).build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        when(productVariantRepository.findById(500L)).thenReturn(Optional.of(sampleVariant));

        BusinessException exception = assertThrows(BusinessException.class, () -> cartService.addToCart(1L, request));
        assertEquals(CartErrorCode.INSUFFICIENT_STOCK.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    @DisplayName("Cập nhật số lượng item thành công")
    void updateCartItem_Success() {
        CartItem existingItem = CartItem.builder().id(1000L).cart(sampleCart).variant(sampleVariant).quantity(2).build();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(5).build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        when(cartItemRepository.findById(1000L)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(existingItem));

        CartResponse response = cartService.updateCartItem(1L, 1000L, request);

        assertNotNull(response);
        assertEquals(5, existingItem.getQuantity());
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    @DisplayName("Xóa toàn bộ sản phẩm trong giỏ hàng thành công")
    void clearCart_Success() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));

        cartService.clearCart(1L);

        verify(cartItemRepository).deleteAllByCartId(10L);
    }
}
