package com.techmart.core.service.query.cart.impl;

import com.techmart.core.dto.response.cart.CartItemResponse;
import com.techmart.core.dto.response.cart.CartResponse;
import com.techmart.core.entity.cart.CartEntity;
import com.techmart.core.entity.cart.CartItemEntity;
import com.techmart.core.entity.catalog.ProductVariantEntity;
import com.techmart.core.entity.inventory.InventoryEntity;
import com.techmart.core.repository.cart.CartItemRepository;
import com.techmart.core.repository.cart.CartRepository;
import com.techmart.core.repository.inventory.InventoryRepository;
import com.techmart.core.service.query.cart.CartQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CartQueryServiceImpl implements CartQueryService {

    private static final Logger log = LoggerFactory.getLogger(CartQueryServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;

    public CartQueryServiceImpl(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            InventoryRepository inventoryRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public CartResponse getCart(UUID customerId) {
        log.info("[CartQueryServiceImpl][getCart] Fetching cart for customer: {}", customerId);

        CartEntity cart = cartRepository.findByCustomerId(customerId)
                .orElse(null);

        if (cart == null) {
            return new CartResponse(null, List.of(), 0, BigDecimal.ZERO);
        }

        List<CartItemEntity> items = cartItemRepository.findByCartIdWithVariantAndProduct(cart.getId());
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal estimatedTotalAmount = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItemEntity item : items) {
            ProductVariantEntity variant = item.getVariant();
            BigDecimal currentPrice = variant.getPrice();
            BigDecimal discountPercent = variant.getDiscountPercent() != null ? variant.getDiscountPercent() : BigDecimal.ZERO;

            BigDecimal discountFactor = BigDecimal.ONE.subtract(
                    discountPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            );
            BigDecimal effectiveUnitPrice = currentPrice.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemSubtotal = effectiveUnitPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);

            List<InventoryEntity> inventories = inventoryRepository.findByVariantId(variant.getId());
            int totalAvailable = inventories.stream()
                    .mapToInt(inv -> inv.getQuantityOnHand() - inv.getQuantityReserved())
                    .sum();
            boolean inStock = totalAvailable >= item.getQuantity();

            itemResponses.add(new CartItemResponse(
                    item.getId(),
                    variant.getId(),
                    variant.getProduct().getName(),
                    variant.getSku(),
                    variant.getAttributes(),
                    variant.getImageUrl(),
                    currentPrice,
                    discountPercent,
                    effectiveUnitPrice,
                    item.getQuantity(),
                    itemSubtotal,
                    inStock
            ));

            totalItems += item.getQuantity();
            estimatedTotalAmount = estimatedTotalAmount.add(itemSubtotal);
        }

        return new CartResponse(cart.getId(), itemResponses, totalItems, estimatedTotalAmount);
    }
}
