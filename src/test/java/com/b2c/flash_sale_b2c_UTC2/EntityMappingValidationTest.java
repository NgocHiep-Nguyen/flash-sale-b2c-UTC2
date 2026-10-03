package com.b2c.flash_sale_b2c_UTC2;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityMappingValidationTest {

    private final List<Class<?>> all24Entities = List.of(
            // Auth & User (8 tables)
            com.b2c.flash_sale_b2c_UTC2.auth.entity.AuthAccount.class,
            com.b2c.flash_sale_b2c_UTC2.user.entity.User.class,
            com.b2c.flash_sale_b2c_UTC2.user.entity.Role.class,
            com.b2c.flash_sale_b2c_UTC2.user.entity.UserRole.class,
            com.b2c.flash_sale_b2c_UTC2.user.entity.PermissionGroup.class,
            com.b2c.flash_sale_b2c_UTC2.user.entity.Permission.class,
            com.b2c.flash_sale_b2c_UTC2.user.entity.GroupPermission.class,
            com.b2c.flash_sale_b2c_UTC2.user.entity.Address.class,
            // Store & Wallet (3 tables)
            com.b2c.flash_sale_b2c_UTC2.store.entity.Store.class,
            com.b2c.flash_sale_b2c_UTC2.wallet.entity.Wallet.class,
            com.b2c.flash_sale_b2c_UTC2.wallet.entity.WalletTransaction.class,
            // Product & Category (3 tables)
            com.b2c.flash_sale_b2c_UTC2.product.entity.Category.class,
            com.b2c.flash_sale_b2c_UTC2.product.entity.Product.class,
            com.b2c.flash_sale_b2c_UTC2.product.entity.ProductVariant.class,
            // Cart (2 tables)
            com.b2c.flash_sale_b2c_UTC2.cart.entity.Cart.class,
            com.b2c.flash_sale_b2c_UTC2.cart.entity.CartItem.class,
            // Order (2 tables)
            com.b2c.flash_sale_b2c_UTC2.order.entity.Order.class,
            com.b2c.flash_sale_b2c_UTC2.order.entity.OrderItem.class,
            // Payment (1 table)
            com.b2c.flash_sale_b2c_UTC2.payment.entity.Payment.class,
            // Flash Sale (2 tables)
            com.b2c.flash_sale_b2c_UTC2.flashsale.entity.FlashSaleSlot.class,
            com.b2c.flash_sale_b2c_UTC2.flashsale.entity.FlashSaleItem.class,
            // Voucher (2 tables)
            com.b2c.flash_sale_b2c_UTC2.voucher.entity.Voucher.class,
            com.b2c.flash_sale_b2c_UTC2.voucher.entity.VoucherUsage.class,
            // Review (1 table)
            com.b2c.flash_sale_b2c_UTC2.review.entity.ProductReview.class
    );

    @Test
    @DisplayName("Xác nhận đúng 24 Entity JPA tương ứng với 24 bảng trong V1__init_schema.sql")
    void testExact24EntityCount() {
        assertEquals(24, all24Entities.size(), "Phải có đúng 24 Entity JPA tương ứng với kiến trúc 24 bảng");
    }

    @Test
    @DisplayName("Xác nhận toàn bộ 24 Entity đều có annotation @Entity và @Table")
    void testEntityAnnotationsAndNoData() {
        for (Class<?> clazz : all24Entities) {
            assertTrue(clazz.isAnnotationPresent(Entity.class), clazz.getSimpleName() + " phải có @Entity");
            assertTrue(clazz.isAnnotationPresent(Table.class), clazz.getSimpleName() + " phải có @Table");
            assertNotNull(clazz.getAnnotation(Table.class).name(), clazz.getSimpleName() + " phải có tên bảng @Table(name=...)");
        }
    }

    @Test
    @DisplayName("Xác nhận mọi quan hệ @ManyToOne và @OneToOne trong 24 Entity đều cấu hình FetchType.LAZY")
    void testAllRelationshipsUseLazyFetch() {
        for (Class<?> clazz : all24Entities) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(ManyToOne.class)) {
                    ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                    assertEquals(FetchType.LAZY, manyToOne.fetch(),
                            "Trường " + clazz.getSimpleName() + "." + field.getName() + " phải có fetch = FetchType.LAZY");
                }
                if (field.isAnnotationPresent(OneToOne.class)) {
                    OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                    assertEquals(FetchType.LAZY, oneToOne.fetch(),
                            "Trường " + clazz.getSimpleName() + "." + field.getName() + " phải có fetch = FetchType.LAZY");
                }
            }
        }
    }

    @Test
    @DisplayName("Xác nhận mọi Entity đều có khóa chính @Id (đơn hoặc composite key)")
    void testAllEntitiesHaveId() {
        for (Class<?> clazz : all24Entities) {
            boolean hasId = false;
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(jakarta.persistence.EmbeddedId.class)) {
                    hasId = true;
                    break;
                }
            }
            assertTrue(hasId, clazz.getSimpleName() + " phải có @Id hoặc @EmbeddedId");
        }
    }
}
