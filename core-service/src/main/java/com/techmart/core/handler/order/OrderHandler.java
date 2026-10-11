package com.techmart.core.handler.order;

import com.techmart.core.bus.CommandHandler;
import com.techmart.core.bus.QueryHandler;
import com.techmart.core.bus.command.order.CancelOrderCommand;
import com.techmart.core.bus.command.order.CreateOrderCommand;
import com.techmart.core.bus.command.order.UpdateOrderStatusCommand;
import com.techmart.core.bus.query.order.GetMyOrdersQuery;
import com.techmart.core.bus.query.order.GetOrderDetailQuery;
import com.techmart.core.bus.query.order.GetOrdersQuery;
import com.techmart.core.dto.response.order.OrderResponse;
import com.techmart.core.dto.response.order.OrderSummaryResponse;
import com.techmart.core.service.command.order.OrderCommandService;
import com.techmart.core.service.query.order.OrderQueryService;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.stereotype.Component;

public class OrderHandler {

    @Component
    public static class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand, OrderResponse> {
        private final OrderCommandService orderCommandService;

        public CreateOrderCommandHandler(OrderCommandService orderCommandService) {
            this.orderCommandService = orderCommandService;
        }

        @Override
        public OrderResponse handle(CreateOrderCommand command) {
            return orderCommandService.createOrder(command.customerId(), command.request());
        }
    }

    @Component
    public static class UpdateOrderStatusCommandHandler implements CommandHandler<UpdateOrderStatusCommand, OrderResponse> {
        private final OrderCommandService orderCommandService;

        public UpdateOrderStatusCommandHandler(OrderCommandService orderCommandService) {
            this.orderCommandService = orderCommandService;
        }

        @Override
        public OrderResponse handle(UpdateOrderStatusCommand command) {
            return orderCommandService.updateOrderStatus(command.orderId(), command.request());
        }
    }

    @Component
    public static class CancelOrderCommandHandler implements CommandHandler<CancelOrderCommand, OrderResponse> {
        private final OrderCommandService orderCommandService;

        public CancelOrderCommandHandler(OrderCommandService orderCommandService) {
            this.orderCommandService = orderCommandService;
        }

        @Override
        public OrderResponse handle(CancelOrderCommand command) {
            return orderCommandService.cancelOrder(command.orderId(), command.request(), command.customerId(), command.role());
        }
    }

    @Component
    public static class GetMyOrdersQueryHandler implements QueryHandler<GetMyOrdersQuery, PagedResponse<OrderSummaryResponse>> {
        private final OrderQueryService orderQueryService;

        public GetMyOrdersQueryHandler(OrderQueryService orderQueryService) {
            this.orderQueryService = orderQueryService;
        }

        @Override
        public PagedResponse<OrderSummaryResponse> handle(GetMyOrdersQuery query) {
            return orderQueryService.getMyOrders(query.customerId(), query.pageable());
        }
    }

    @Component
    public static class GetOrderDetailQueryHandler implements QueryHandler<GetOrderDetailQuery, OrderResponse> {
        private final OrderQueryService orderQueryService;

        public GetOrderDetailQueryHandler(OrderQueryService orderQueryService) {
            this.orderQueryService = orderQueryService;
        }

        @Override
        public OrderResponse handle(GetOrderDetailQuery query) {
            return orderQueryService.getOrderDetail(query.orderId(), query.customerId(), query.role());
        }
    }

    @Component
    public static class GetOrdersQueryHandler implements QueryHandler<GetOrdersQuery, PagedResponse<OrderSummaryResponse>> {
        private final OrderQueryService orderQueryService;

        public GetOrdersQueryHandler(OrderQueryService orderQueryService) {
            this.orderQueryService = orderQueryService;
        }

        @Override
        public PagedResponse<OrderSummaryResponse> handle(GetOrdersQuery query) {
            return orderQueryService.getOrders(query.status(), query.pageable());
        }
    }
}
