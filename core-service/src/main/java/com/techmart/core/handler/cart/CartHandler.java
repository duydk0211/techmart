package com.techmart.core.handler.cart;

import com.techmart.core.bus.CommandHandler;
import com.techmart.core.bus.QueryHandler;
import com.techmart.core.bus.command.cart.AddToCartCommand;
import com.techmart.core.bus.command.cart.ClearCartCommand;
import com.techmart.core.bus.command.cart.RemoveCartItemCommand;
import com.techmart.core.bus.command.cart.UpdateCartItemCommand;
import com.techmart.core.bus.query.cart.GetCartQuery;
import com.techmart.core.dto.response.cart.CartResponse;
import com.techmart.core.service.command.cart.CartCommandService;
import com.techmart.core.service.query.cart.CartQueryService;
import org.springframework.stereotype.Component;

public class CartHandler {

    @Component
    public static class AddToCartCommandHandler implements CommandHandler<AddToCartCommand, CartResponse> {
        private final CartCommandService cartCommandService;

        public AddToCartCommandHandler(CartCommandService cartCommandService) {
            this.cartCommandService = cartCommandService;
        }

        @Override
        public CartResponse handle(AddToCartCommand command) {
            return cartCommandService.addToCart(command.customerId(), command.request());
        }
    }

    @Component
    public static class UpdateCartItemCommandHandler implements CommandHandler<UpdateCartItemCommand, CartResponse> {
        private final CartCommandService cartCommandService;

        public UpdateCartItemCommandHandler(CartCommandService cartCommandService) {
            this.cartCommandService = cartCommandService;
        }

        @Override
        public CartResponse handle(UpdateCartItemCommand command) {
            return cartCommandService.updateCartItem(command.customerId(), command.itemId(), command.request());
        }
    }

    @Component
    public static class RemoveCartItemCommandHandler implements CommandHandler<RemoveCartItemCommand, Void> {
        private final CartCommandService cartCommandService;

        public RemoveCartItemCommandHandler(CartCommandService cartCommandService) {
            this.cartCommandService = cartCommandService;
        }

        @Override
        public Void handle(RemoveCartItemCommand command) {
            cartCommandService.removeCartItem(command.customerId(), command.itemId());
            return null;
        }
    }

    @Component
    public static class ClearCartCommandHandler implements CommandHandler<ClearCartCommand, Void> {
        private final CartCommandService cartCommandService;

        public ClearCartCommandHandler(CartCommandService cartCommandService) {
            this.cartCommandService = cartCommandService;
        }

        @Override
        public Void handle(ClearCartCommand command) {
            cartCommandService.clearCart(command.customerId());
            return null;
        }
    }

    @Component
    public static class GetCartQueryHandler implements QueryHandler<GetCartQuery, CartResponse> {
        private final CartQueryService cartQueryService;

        public GetCartQueryHandler(CartQueryService cartQueryService) {
            this.cartQueryService = cartQueryService;
        }

        @Override
        public CartResponse handle(GetCartQuery query) {
            return cartQueryService.getCart(query.customerId());
        }
    }
}
