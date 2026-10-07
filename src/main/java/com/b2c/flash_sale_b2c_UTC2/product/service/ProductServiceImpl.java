package com.b2c.flash_sale_b2c_UTC2.product.service;

import com.b2c.flash_sale_b2c_UTC2.common.api.PageResponse;
import com.b2c.flash_sale_b2c_UTC2.common.exception.BusinessException;
import com.b2c.flash_sale_b2c_UTC2.flashsale.repository.FlashSaleItemRepository;
import com.b2c.flash_sale_b2c_UTC2.order.repository.OrderItemRepository;
import com.b2c.flash_sale_b2c_UTC2.product.dto.CreateProductRequest;
import com.b2c.flash_sale_b2c_UTC2.product.dto.CreateProductVariantRequest;
import com.b2c.flash_sale_b2c_UTC2.product.dto.ProductDetailResponse;
import com.b2c.flash_sale_b2c_UTC2.product.dto.ProductFilterRequest;
import com.b2c.flash_sale_b2c_UTC2.product.dto.ProductSummaryResponse;
import com.b2c.flash_sale_b2c_UTC2.product.dto.ProductVariantResponse;
import com.b2c.flash_sale_b2c_UTC2.product.dto.TierVariationConfigDto;
import com.b2c.flash_sale_b2c_UTC2.product.dto.UpdateProductRequest;
import com.b2c.flash_sale_b2c_UTC2.product.dto.UpdateProductVariantRequest;
import com.b2c.flash_sale_b2c_UTC2.product.entity.Category;
import com.b2c.flash_sale_b2c_UTC2.product.entity.Product;
import com.b2c.flash_sale_b2c_UTC2.product.entity.ProductVariant;
import com.b2c.flash_sale_b2c_UTC2.product.exception.ProductErrorCode;
import com.b2c.flash_sale_b2c_UTC2.product.mapper.ProductVariantMapper;
import com.b2c.flash_sale_b2c_UTC2.product.repository.CategoryRepository;
import com.b2c.flash_sale_b2c_UTC2.product.repository.ProductRepository;
import com.b2c.flash_sale_b2c_UTC2.product.repository.ProductVariantRepository;
import com.b2c.flash_sale_b2c_UTC2.store.entity.Store;
import com.b2c.flash_sale_b2c_UTC2.store.exception.StoreErrorCode;
import com.b2c.flash_sale_b2c_UTC2.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final OrderItemRepository orderItemRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final ProductVariantMapper productVariantMapper;

    @Override
    @Transactional
    public ProductDetailResponse createProduct(Long userId, CreateProductRequest request) {
        Store store = getApprovedSellerStore(userId);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));

        validateTierVariations(request.getTierVariationConfigs(), request.getVariants().size());
        validateCreateVariants(request.getVariants());

        Product product = Product.builder()
                .store(store)
                .category(category)
                .name(request.getName().trim())
                .description(request.getDescription())
                .tierVariationConfigs(request.getTierVariationConfigs())
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();

        Product savedProduct = productRepository.save(product);

        List<ProductVariant> variants = new ArrayList<>();
        for (CreateProductVariantRequest vReq : request.getVariants()) {
            ProductVariant variant = ProductVariant.builder()
                    .product(savedProduct)
                    .sku(vReq.getSku().trim())
                    .variantName(vReq.getVariantName().trim())
                    .attributes(vReq.getAttributes())
                    .originalPrice(vReq.getOriginalPrice())
                    .stockQuantity(vReq.getStockQuantity())
                    .status("ACTIVE")
                    .version(0L)
                    .createdAt(Instant.now())
                    .build();
            variants.add(variant);
        }

        List<ProductVariant> savedVariants = productVariantRepository.saveAll(variants);

        return buildProductDetailResponse(savedProduct, savedVariants);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(Long userId, Long productId, UpdateProductRequest request) {
        Store store = getApprovedSellerStore(userId);

        Product product = productRepository.findByIdWithStoreAndCategory(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getStore().getId().equals(store.getId())) {
            throw new BusinessException(ProductErrorCode.PRODUCT_ACCESS_DENIED);
        }

        if (!product.getCategory().getId().equals(request.getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));
            product.setCategory(category);
        }

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            validateTierVariations(request.getTierVariationConfigs(), request.getVariants().size());
            product.setTierVariationConfigs(request.getTierVariationConfigs());

            updateProductVariants(product, request.getVariants());
        }

        Product updatedProduct = productRepository.save(product);
        List<ProductVariant> currentVariants = productVariantRepository.findByProductId(updatedProduct.getId());

        return buildProductDetailResponse(updatedProduct, currentVariants);
    }

    private void updateProductVariants(Product product, List<UpdateProductVariantRequest> variantRequests) {
        Set<String> skuSet = new HashSet<>();
        for (UpdateProductVariantRequest vReq : variantRequests) {
            if (!skuSet.add(vReq.getSku().trim().toLowerCase())) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_SKU_IN_REQUEST);
            }
            if (vReq.getOriginalPrice() == null || vReq.getOriginalPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ProductErrorCode.INVALID_PRODUCT_PRICE);
            }
            if (vReq.getStockQuantity() == null || vReq.getStockQuantity() < 0) {
                throw new BusinessException(ProductErrorCode.INVALID_STOCK_QUANTITY);
            }
        }

        List<ProductVariant> existingVariants = productVariantRepository.findByProductId(product.getId());
        Map<Long, ProductVariant> existingMap = existingVariants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        List<ProductVariant> toSave = new ArrayList<>();
        for (UpdateProductVariantRequest vReq : variantRequests) {
            if (vReq.getId() != null) {
                ProductVariant existing = existingMap.get(vReq.getId());
                if (existing == null) {
                    throw new BusinessException(ProductErrorCode.VARIANT_NOT_FOUND);
                }

                if (productVariantRepository.existsBySkuAndIdNot(vReq.getSku().trim(), existing.getId())) {
                    throw new BusinessException(ProductErrorCode.SKU_ALREADY_EXISTS);
                }

                // Kiểm tra nếu biến thể đang trong Flash Sale ACTIVE thì cấm sửa giá hoặc tồn kho
                boolean inActiveFlashSale = flashSaleItemRepository.isVariantInActiveFlashSale(existing.getId());
                if (inActiveFlashSale) {
                    boolean priceChanged = existing.getOriginalPrice().compareTo(vReq.getOriginalPrice()) != 0;
                    boolean stockDecreased = vReq.getStockQuantity() < existing.getStockQuantity();
                    if (priceChanged || stockDecreased) {
                        throw new BusinessException(ProductErrorCode.VARIANT_IN_ACTIVE_FLASH_SALE);
                    }
                }

                existing.setSku(vReq.getSku().trim());
                existing.setVariantName(vReq.getVariantName().trim());
                existing.setAttributes(vReq.getAttributes());
                existing.setOriginalPrice(vReq.getOriginalPrice());
                existing.setStockQuantity(vReq.getStockQuantity());
                if (vReq.getStatus() != null) {
                    existing.setStatus(vReq.getStatus());
                }
                toSave.add(existing);
            } else {
                // Tạo mới biến thể bổ sung vào SPU
                if (productVariantRepository.existsBySku(vReq.getSku().trim())) {
                    throw new BusinessException(ProductErrorCode.SKU_ALREADY_EXISTS);
                }
                ProductVariant newVariant = ProductVariant.builder()
                        .product(product)
                        .sku(vReq.getSku().trim())
                        .variantName(vReq.getVariantName().trim())
                        .attributes(vReq.getAttributes())
                        .originalPrice(vReq.getOriginalPrice())
                        .stockQuantity(vReq.getStockQuantity())
                        .status(vReq.getStatus() != null ? vReq.getStatus() : "ACTIVE")
                        .version(0L)
                        .createdAt(Instant.now())
                        .build();
                toSave.add(newVariant);
            }
        }

        productVariantRepository.saveAll(toSave);
    }

    @Override
    @Transactional
    public void deleteProduct(Long userId, Long productId) {
        Store store = getApprovedSellerStore(userId);

        Product product = productRepository.findByIdWithStoreAndCategory(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getStore().getId().equals(store.getId())) {
            throw new BusinessException(ProductErrorCode.PRODUCT_ACCESS_DENIED);
        }

        List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
        List<Long> variantIds = variants.stream().map(ProductVariant::getId).toList();

        boolean hasOrders = !variantIds.isEmpty() && orderItemRepository.existsByVariantIdIn(variantIds);
        boolean hasFlashSales = !variantIds.isEmpty() && flashSaleItemRepository.existsByVariantIdIn(variantIds);

        if (hasOrders || hasFlashSales) {
            log.info("Sản phẩm ID {} hoặc các biến thể đã có đơn hàng/flash sale. Tiến hành Soft Delete (INACTIVE).", productId);
            product.setStatus("INACTIVE");
            productRepository.save(product);

            for (ProductVariant v : variants) {
                v.setStatus("INACTIVE");
            }
            productVariantRepository.saveAll(variants);
        } else {
            log.info("Sản phẩm ID {} chưa phát sinh giao dịch. Tiến hành Hard Delete.", productId);
            productVariantRepository.deleteAll(variants);
            productRepository.delete(product);
        }
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProductStatus(Long userId, Long productId, String status) {
        Store store = getApprovedSellerStore(userId);

        Product product = productRepository.findByIdWithStoreAndCategory(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getStore().getId().equals(store.getId())) {
            throw new BusinessException(ProductErrorCode.PRODUCT_ACCESS_DENIED);
        }

        product.setStatus(status.toUpperCase());
        Product saved = productRepository.save(product);
        List<ProductVariant> variants = productVariantRepository.findByProductId(saved.getId());

        return buildProductDetailResponse(saved, variants);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getPublicProducts(ProductFilterRequest filter, Pageable pageable) {
        // NOTE: Sentinel values to avoid Hibernate binding null as bytea (Hibernate6 + Postgres driver bug).
        // JPQL uses `:categoryId < 0`, `:keyword = ''`, `:minPrice < 0`, `:maxPrice < 0` as "filter disabled" markers.
        Integer categoryId = (filter.getCategoryId() != null && filter.getCategoryId() > 0)
                ? filter.getCategoryId() : -1;
        String keyword = (filter.getKeyword() != null && !filter.getKeyword().isBlank())
                ? filter.getKeyword().trim() : "";
        BigDecimal minPrice = filter.getMinPrice() != null ? filter.getMinPrice() : BigDecimal.valueOf(-1);
        BigDecimal maxPrice = filter.getMaxPrice() != null ? filter.getMaxPrice() : BigDecimal.valueOf(-1);

        Page<Product> productPage = productRepository.findPublicProducts(
                categoryId,
                keyword,
                minPrice,
                maxPrice,
                pageable
        );

        List<Long> productIds = productPage.getContent().stream().map(Product::getId).toList();
        Map<Long, List<ProductVariant>> variantsByProductId = productIds.isEmpty() ? Map.of() :
                productVariantRepository.findByProductIdInAndStatus(productIds, "ACTIVE").stream()
                        .collect(Collectors.groupingBy(v -> v.getProduct().getId()));

        List<ProductSummaryResponse> content = productPage.getContent().stream()
                .map(p -> mapToSummaryResponse(p, variantsByProductId.getOrDefault(p.getId(), List.of())))
                .toList();

        return PageResponse.of(productPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getPublicProductDetail(Long productId) {
        Product product = productRepository.findByIdWithStoreAndCategory(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (!"ACTIVE".equalsIgnoreCase(product.getStatus()) || !"APPROVED".equalsIgnoreCase(product.getStore().getStatus())) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }

        List<ProductVariant> activeVariants = productVariantRepository.findByProductIdAndStatus(productId, "ACTIVE");

        return buildProductDetailResponse(product, activeVariants);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getSellerProducts(Long userId, String status, Pageable pageable) {
        Store store = getApprovedSellerStore(userId);

        Page<Product> productPage = productRepository.findSellerProducts(store.getId(), status, pageable);

        List<Long> productIds = productPage.getContent().stream().map(Product::getId).toList();
        Map<Long, List<ProductVariant>> variantsByProductId = productIds.isEmpty() ? Map.of() :
                productVariantRepository.findByProductIdInAndStatus(productIds, "ACTIVE").stream()
                        .collect(Collectors.groupingBy(v -> v.getProduct().getId()));

        List<ProductSummaryResponse> content = productPage.getContent().stream()
                .map(p -> mapToSummaryResponse(p, variantsByProductId.getOrDefault(p.getId(), List.of())))
                .toList();

        return PageResponse.of(productPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getSellerProductDetail(Long userId, Long productId) {
        Store store = getApprovedSellerStore(userId);

        Product product = productRepository.findByIdWithStoreAndCategory(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getStore().getId().equals(store.getId())) {
            throw new BusinessException(ProductErrorCode.PRODUCT_ACCESS_DENIED);
        }

        List<ProductVariant> allVariants = productVariantRepository.findByProductId(productId);

        return buildProductDetailResponse(product, allVariants);
    }

    private Store getApprovedSellerStore(Long userId) {
        Store store = storeRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));

        if (!"APPROVED".equalsIgnoreCase(store.getStatus())) {
            throw new BusinessException(ProductErrorCode.STORE_NOT_ELIGIBLE);
        }

        return store;
    }

    private void validateTierVariations(List<TierVariationConfigDto> configs, int variantCount) {
        if (configs == null || configs.isEmpty()) {
            if (variantCount != 1) {
                throw new BusinessException(ProductErrorCode.INVALID_TIER_VARIATION);
            }
            return;
        }

        int expectedCount = 1;
        for (TierVariationConfigDto config : configs) {
            if (config.getOptions() == null || config.getOptions().isEmpty()) {
                throw new BusinessException(ProductErrorCode.INVALID_TIER_VARIATION);
            }
            expectedCount *= config.getOptions().size();
        }

        if (variantCount != expectedCount) {
            log.warn("Số biến thể gửi lên ({}) không khớp với số biến thể dự kiến ({}) từ phân loại", variantCount, expectedCount);
            throw new BusinessException(ProductErrorCode.INVALID_TIER_VARIATION);
        }
    }

    private void validateCreateVariants(List<CreateProductVariantRequest> variantRequests) {
        Set<String> skuSet = new HashSet<>();
        for (CreateProductVariantRequest vReq : variantRequests) {
            String lowerSku = vReq.getSku().trim().toLowerCase();
            if (!skuSet.add(lowerSku)) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_SKU_IN_REQUEST);
            }

            if (vReq.getOriginalPrice() == null || vReq.getOriginalPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ProductErrorCode.INVALID_PRODUCT_PRICE);
            }

            if (vReq.getStockQuantity() == null || vReq.getStockQuantity() < 0) {
                throw new BusinessException(ProductErrorCode.INVALID_STOCK_QUANTITY);
            }

            if (productVariantRepository.existsBySku(vReq.getSku().trim())) {
                throw new BusinessException(ProductErrorCode.SKU_ALREADY_EXISTS);
            }
        }
    }

    private ProductSummaryResponse mapToSummaryResponse(Product product, List<ProductVariant> variants) {
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        int totalStock = 0;

        for (ProductVariant v : variants) {
            if (minPrice == null || v.getOriginalPrice().compareTo(minPrice) < 0) {
                minPrice = v.getOriginalPrice();
            }
            if (maxPrice == null || v.getOriginalPrice().compareTo(maxPrice) > 0) {
                maxPrice = v.getOriginalPrice();
            }
            totalStock += (v.getStockQuantity() != null ? v.getStockQuantity() : 0);
        }

        return ProductSummaryResponse.builder()
                .id(product.getId())
                .storeId(product.getStore().getId())
                .storeName(product.getStore().getStoreName())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .totalStock(totalStock)
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
    }

    private ProductDetailResponse buildProductDetailResponse(Product product, List<ProductVariant> variants) {
        List<ProductVariantResponse> variantResponses = variants.stream()
                .map(productVariantMapper::toResponse)
                .toList();

        return ProductDetailResponse.builder()
                .id(product.getId())
                .storeId(product.getStore().getId())
                .storeName(product.getStore().getStoreName())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .description(product.getDescription())
                .tierVariationConfigs(product.getTierVariationConfigs())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .variants(variantResponses)
                .build();
    }
}
