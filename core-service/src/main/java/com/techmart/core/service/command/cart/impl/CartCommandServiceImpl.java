package com.techmart.core.service.command.cart.impl;

import com.techmart.core.dto.request.cart.AddToCartRequest;
import com.techmart.core.dto.request.cart.UpdateCartItemRequest;
import com.techmart.core.dto.response.cart.CartResponse;
import com.techmart.core.entity.cart.CartEntity;
import com.techmart.core.entity.cart.CartItemEntity;
import com.techmart.core.entity.catalog.ProductVariantEntity;
import com.techmart.core.repository.cart.CartItemRepository;
import com.techmart.core.repository.cart.CartRepository;
import com.techmart.core.repository.catalog.ProductVariantRepository;
import com.techmart.core.service.command.cart.CartCommandService;
import com.techmart.core.service.query.cart.CartQueryService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CartCommandServiceImpl implements CartCommandService {

    private static final Logger log = LoggerFactory.getLogger(CartCommandServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartQueryService cartQueryService;

    public CartCommandServiceImpl(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductVariantRepository productVariantRepository,
            CartQueryService cartQueryService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.cartQueryService = cartQueryService;
    }

    @Override
    public CartResponse addToCart(UUID customerId, AddToCartRequest request) {
        log.info("[CartCommandServiceImpl][addToCart] Adding variant {} with qty {} for customer {}",
                request.variantId(), request.quantity(), customerId);

        ProductVariantEntity variant = productVariantRepository.findById(request.variantId())
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Product variant not found: " + request.variantId()));

        if (!"ACTIVE".equalsIgnoreCase(variant.getStatus())) {
            throw new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Product variant is not active");
        }

        CartEntity cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(new CartEntity(customerId)));

        CartItemEntity item = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId())
                .orElse(null);

        if (item != null) {
            item.setQuantity(item.getQuantity() + request.quantity());
            cartItemRepository.save(item);
        } else {
            CartItemEntity newItem = new CartItemEntity(cart, variant, request.quantity());
            cartItemRepository.save(newItem);
        }

        return cartQueryService.getCart(customerId);
    }

    @Override
    public CartResponse updateCartItem(UUID customerId, UUID itemId, UpdateCartItemRequest request) {
        log.info("[CartCommandServiceImpl][updateCartItem] Updating cart item {} to qty {} for customer {}",
                itemId, request.quantity(), customerId);

        if (request.quantity() <= 0) {
            throw new CoreException(ResponseCode.VALIDATION_FAILED, "Quantity must be greater than 0");
        }

        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Cart item not found: " + itemId));

        if (!item.getCart().getCustomerId().equals(customerId)) {
            log.warn("[CartCommandServiceImpl][updateCartItem] Forbidden access: customer {} does not own cart item {}",
                    customerId, itemId);
            throw new CoreException(ResponseCode.FORBIDDEN_RESOURCE, "You do not own this cart item");
        }

        item.setQuantity(request.quantity());
        cartItemRepository.save(item);

        return cartQueryService.getCart(customerId);
    }

    @Override
    public void removeCartItem(UUID customerId, UUID itemId) {
        log.info("[CartCommandServiceImpl][removeCartItem] Removing cart item {} for customer {}", itemId, customerId);

        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Cart item not found: " + itemId));

        if (!item.getCart().getCustomerId().equals(customerId)) {
            log.warn("[CartCommandServiceImpl][removeCartItem] Forbidden access: customer {} does not own cart item {}",
                    customerId, itemId);
            throw new CoreException(ResponseCode.FORBIDDEN_RESOURCE, "You do not own this cart item");
        }

        cartItemRepository.delete(item);
    }

    @Override
    public void clearCart(UUID customerId) {
        log.info("[CartCommandServiceImpl][clearCart] Clearing cart for customer {}", customerId);

        cartRepository.findByCustomerId(customerId)
                .ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getId()));
    }
}
