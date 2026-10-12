package com.techmart.core.service.command.order.impl;

import com.fasterxml.uuid.Generators;
import com.techmart.core.dto.request.order.CancelOrderRequest;
import com.techmart.core.dto.request.order.CreateOrderRequest;
import com.techmart.core.dto.request.order.NewAddressRequest;
import com.techmart.core.dto.request.order.OrderItemRequest;
import com.techmart.core.dto.request.order.UpdateOrderStatusRequest;
import com.techmart.core.dto.response.order.OrderResponse;
import com.techmart.core.entity.branch.BranchEntity;
import com.techmart.core.entity.catalog.ProductVariantEntity;
import com.techmart.core.entity.inventory.InventoryEntity;
import com.techmart.core.entity.inventory.InventoryHistoryEntity;
import com.techmart.core.entity.order.OrderEntity;
import com.techmart.core.entity.order.OrderItemEntity;
import com.techmart.core.entity.order.OrderStatus;
import com.techmart.core.entity.user.AddressItem;
import com.techmart.core.entity.user.UserEntity;
import com.techmart.core.repository.branch.BranchRepository;
import com.techmart.core.repository.cart.CartItemRepository;
import com.techmart.core.repository.cart.CartRepository;
import com.techmart.core.repository.catalog.ProductVariantRepository;
import com.techmart.core.repository.inventory.InventoryHistoryRepository;
import com.techmart.core.repository.inventory.InventoryRepository;
import com.techmart.core.repository.order.OrderRepository;
import com.techmart.core.repository.user.UserRepository;
import com.techmart.core.service.command.order.OrderCommandService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderCommandServiceImpl implements OrderCommandService {

    private static final Logger log = LoggerFactory.getLogger(OrderCommandServiceImpl.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public OrderCommandServiceImpl(
            OrderRepository orderRepository,
            UserRepository userRepository,
            BranchRepository branchRepository,
            ProductVariantRepository productVariantRepository,
            InventoryRepository inventoryRepository,
            InventoryHistoryRepository inventoryHistoryRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryHistoryRepository = inventoryHistoryRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    public OrderResponse createOrder(UUID customerId, CreateOrderRequest request) {
        log.info("[OrderCommandServiceImpl][createOrder] Initiating order creation for customer: {}, branch: {}",
                customerId, request.branchId());

        UserEntity user = userRepository.findById(customerId)
                .orElseThrow(() -> new CoreException(ResponseCode.USER_NOT_FOUND, "Customer not found: " + customerId));

        BranchEntity branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Branch not found: " + request.branchId()));

        String shippingAddress;
        String customerName;
        String customerPhone;
        String customerEmail = user.getEmail();

        if (request.addressId() != null) {
            AddressItem address = user.getUserAddressInfo().stream()
                    .filter(a -> a.id().equals(request.addressId()))
                    .findFirst()
                    .orElseThrow(() -> new CoreException(ResponseCode.ADDRESS_NOT_FOUND, "Address not found: " + request.addressId()));
            shippingAddress = address.street() + ", " + address.province() + ", " + address.city();
            customerName = address.name();
            customerPhone = address.phone();
        } else if (request.newAddress() != null) {
            NewAddressRequest newAddr = request.newAddress();
            shippingAddress = newAddr.street() + ", " + newAddr.province() + ", " + newAddr.city();
            customerName = newAddr.name();
            customerPhone = newAddr.phone();

            if (newAddr.saveToAddressBook()) {
                AddressItem item = new AddressItem(
                        Generators.timeBasedEpochGenerator().generate(),
                        newAddr.name(),
                        newAddr.phone(),
                        newAddr.street(),
                        newAddr.province(),
                        newAddr.city(),
                        false
                );
                user.addAddress(item);
                userRepository.save(user);
            }
        } else {
            throw new CoreException(ResponseCode.VALIDATION_FAILED, "Delivery address or addressId is required");
        }

        // Deadlock Prevention Invariant: sort by variantId ASC
        List<OrderItemRequest> sortedItems = request.items().stream()
                .sorted(Comparator.comparing(OrderItemRequest::variantId))
                .toList();

        String orderNumber = generateUniqueOrderNumber();

        OrderEntity order = new OrderEntity();
        order.setOrderNumber(orderNumber);
        order.setCustomerId(customerId);
        order.setBranch(branch);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(shippingAddress);
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        order.setCustomerEmail(customerEmail);
        order.setNote(request.note());

        BigDecimal totalAmount = BigDecimal.ZERO;
        Instant now = Instant.now();

        // Anti-Overselling Invariant: atomic conditional UPDATE
        for (OrderItemRequest itemReq : sortedItems) {
            ProductVariantEntity variant = productVariantRepository.findById(itemReq.variantId())
                    .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Product variant not found: " + itemReq.variantId()));

            if (!"ACTIVE".equalsIgnoreCase(variant.getStatus())) {
                throw new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Product variant is not active: " + itemReq.variantId());
            }

            int affected = inventoryRepository.reserveStock(branch.getId(), itemReq.variantId(), itemReq.quantity(), now);
            if (affected == 0) {
                log.warn("[OrderCommandServiceImpl][createOrder] Insufficient stock for variant: {} at branch: {}",
                        itemReq.variantId(), branch.getId());
                throw new CoreException(ResponseCode.INSUFFICIENT_STOCK, "Insufficient stock for variant: " + itemReq.variantId());
            }

            InventoryEntity inv = inventoryRepository.findByBranchIdAndVariantId(branch.getId(), itemReq.variantId())
                    .orElse(null);
            int reservedAfter = inv != null ? inv.getQuantityReserved() : itemReq.quantity();
            int reservedBefore = reservedAfter - itemReq.quantity();

            InventoryHistoryEntity history = new InventoryHistoryEntity();
            history.setBranch(branch);
            history.setVariant(variant);
            history.setChangeType("RESERVE");
            history.setQuantityChange(itemReq.quantity());
            history.setQuantityBefore(reservedBefore);
            history.setQuantityAfter(reservedAfter);
            history.setReferenceType("ORDER");
            history.setNote("Reserved for order " + orderNumber);
            inventoryHistoryRepository.save(history);

            // Snapshot Pattern: freeze pricing and attributes
            BigDecimal unitPrice = variant.getPrice();
            BigDecimal discountPercent = variant.getDiscountPercent() != null ? variant.getDiscountPercent() : BigDecimal.ZERO;
            BigDecimal discountFactor = BigDecimal.ONE.subtract(
                    discountPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            );
            BigDecimal effectiveUnitPrice = unitPrice.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = effectiveUnitPrice.multiply(BigDecimal.valueOf(itemReq.quantity())).setScale(2, RoundingMode.HALF_UP);

            totalAmount = totalAmount.add(subtotal);

            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setVariant(variant);
            orderItem.setProductName(variant.getProduct().getName());
            orderItem.setVariantSku(variant.getSku());
            orderItem.setVariantAttributes(new HashMap<>(variant.getAttributes()));
            orderItem.setUnitPrice(unitPrice);
            orderItem.setDiscountPercent(discountPercent);
            orderItem.setQuantity(itemReq.quantity());
            orderItem.setSubtotal(subtotal);

            order.addItem(orderItem);
        }

        order.setTotalAmount(totalAmount);
        OrderEntity savedOrder = orderRepository.save(order);

        // Cart Cleanup
        cartRepository.findByCustomerId(customerId)
                .ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getId()));

        log.info("[OrderCommandServiceImpl][createOrder] Order {} created successfully with ID {}",
                orderNumber, savedOrder.getId());

        return OrderResponse.fromEntity(savedOrder);
    }

    @Override
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        log.info("[OrderCommandServiceImpl][updateOrderStatus] Updating order {} to status: {}",
                orderId, request.status());

        OrderEntity order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Order not found: " + orderId));

        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(request.status().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CoreException(ResponseCode.VALIDATION_FAILED, "Invalid order status: " + request.status());
        }

        OrderStatus currentStatus = order.getStatus();

        // Single-Direction Order State Machine check
        boolean isValidTransition = (currentStatus == OrderStatus.PENDING && targetStatus == OrderStatus.CONFIRMED)
                || (currentStatus == OrderStatus.CONFIRMED && targetStatus == OrderStatus.PROCESSING)
                || (currentStatus == OrderStatus.PROCESSING && targetStatus == OrderStatus.SHIPPED)
                || (currentStatus == OrderStatus.SHIPPED && targetStatus == OrderStatus.DELIVERED);

        if (!isValidTransition) {
            log.warn("[OrderCommandServiceImpl][updateOrderStatus] Invalid transition from {} to {}",
                    currentStatus, targetStatus);
            throw new CoreException(ResponseCode.INVALID_ORDER_STATE,
                    "Invalid order state transition from " + currentStatus + " to " + targetStatus);
        }

        // On successful delivery: deduct physical stock and reserved units
        if (targetStatus == OrderStatus.DELIVERED) {
            List<OrderItemEntity> sortedItems = order.getItems().stream()
                    .filter(i -> i.getVariant() != null)
                    .sorted(Comparator.comparing(i -> i.getVariant().getId()))
                    .toList();

            Instant now = Instant.now();
            for (OrderItemEntity item : sortedItems) {
                inventoryRepository.deductStockOnDelivery(
                        order.getBranch().getId(),
                        item.getVariant().getId(),
                        item.getQuantity(),
                        now
                );

                branchRepository.deductAvailableStock(
                        order.getBranch().getId(),
                        item.getQuantity(),
                        now
                );

                InventoryEntity inv = inventoryRepository.findByBranchIdAndVariantId(
                        order.getBranch().getId(),
                        item.getVariant().getId()
                ).orElse(null);

                int onHandAfter = inv != null ? inv.getQuantityOnHand() : 0;
                int onHandBefore = onHandAfter + item.getQuantity();

                InventoryHistoryEntity history = new InventoryHistoryEntity();
                history.setBranch(order.getBranch());
                history.setVariant(item.getVariant());
                history.setChangeType("SALE_DEDUCT");
                history.setQuantityChange(-item.getQuantity());
                history.setQuantityBefore(onHandBefore);
                history.setQuantityAfter(onHandAfter);
                history.setReferenceType("ORDER");
                history.setReferenceId(order.getId());
                history.setNote("Stock deducted on delivery for order " + order.getOrderNumber());
                inventoryHistoryRepository.save(history);
            }
        }

        order.setStatus(targetStatus);
        if (request.note() != null && !request.note().isBlank()) {
            String updatedNote = order.getNote() != null
                    ? order.getNote() + " | " + request.note()
                    : request.note();
            order.setNote(updatedNote);
        }

        OrderEntity updatedOrder = orderRepository.save(order);
        log.info("[OrderCommandServiceImpl][updateOrderStatus] Order {} transitioned to {}",
                order.getOrderNumber(), targetStatus);

        return OrderResponse.fromEntity(updatedOrder);
    }

    @Override
    public OrderResponse cancelOrder(UUID orderId, CancelOrderRequest request, UUID customerId, String role) {
        log.info("[OrderCommandServiceImpl][cancelOrder] Cancelling order {} requested by user {} (role: {})",
                orderId, customerId, role);

        OrderEntity order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Order not found: " + orderId));

        boolean isStaffOrAdmin = "STAFF".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);

        if (!isStaffOrAdmin) {
            if (!order.getCustomerId().equals(customerId)) {
                log.warn("[OrderCommandServiceImpl][cancelOrder] Forbidden access: customer {} does not own order {}",
                        customerId, orderId);
                throw new CoreException(ResponseCode.FORBIDDEN_RESOURCE, "You do not have access to this order");
            }
            if (order.getStatus() != OrderStatus.PENDING) {
                log.warn("[OrderCommandServiceImpl][cancelOrder] Customer cannot cancel order in status: {}", order.getStatus());
                throw new CoreException(ResponseCode.ORDER_CANNOT_BE_CANCELLED,
                        "Customer can only cancel orders in PENDING status");
            }
        }

        if (order.getStatus() == OrderStatus.SHIPPED) {
            log.warn("[OrderCommandServiceImpl][cancelOrder] Blocked cancellation: Order {} is already SHIPPED",
                    order.getOrderNumber());
            throw new CoreException(ResponseCode.ORDER_CANNOT_BE_CANCELLED,
                    "Order has already been shipped and cannot be cancelled");
        }

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("[OrderCommandServiceImpl][cancelOrder] Cannot cancel order in final status: {}", order.getStatus());
            throw new CoreException(ResponseCode.INVALID_ORDER_STATE,
                    "Cannot cancel order in final status: " + order.getStatus());
        }

        // Release reserved stock with deadlock prevention sorting
        List<OrderItemEntity> sortedItems = order.getItems().stream()
                .filter(i -> i.getVariant() != null)
                .sorted(Comparator.comparing(i -> i.getVariant().getId()))
                .toList();

        Instant now = Instant.now();
        for (OrderItemEntity item : sortedItems) {
            inventoryRepository.releaseStock(
                    order.getBranch().getId(),
                    item.getVariant().getId(),
                    item.getQuantity(),
                    now
            );

            InventoryEntity inv = inventoryRepository.findByBranchIdAndVariantId(
                    order.getBranch().getId(),
                    item.getVariant().getId()
            ).orElse(null);

            int reservedAfter = inv != null ? inv.getQuantityReserved() : 0;
            int reservedBefore = reservedAfter + item.getQuantity();

            InventoryHistoryEntity history = new InventoryHistoryEntity();
            history.setBranch(order.getBranch());
            history.setVariant(item.getVariant());
            history.setChangeType("RELEASE");
            history.setQuantityChange(-item.getQuantity());
            history.setQuantityBefore(reservedBefore);
            history.setQuantityAfter(reservedAfter);
            history.setReferenceType("ORDER");
            history.setReferenceId(order.getId());
            history.setNote("Released stock on cancellation for order " + order.getOrderNumber());
            inventoryHistoryRepository.save(history);
        }

        order.setStatus(OrderStatus.CANCELLED);
        String cancelReason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason()
                : "Cancelled by user";
        order.setCancelledReason(cancelReason);

        OrderEntity updatedOrder = orderRepository.save(order);
        log.info("[OrderCommandServiceImpl][cancelOrder] Order {} cancelled successfully", order.getOrderNumber());

        return OrderResponse.fromEntity(updatedOrder);
    }

    private String generateUniqueOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNumber;
        do {
            String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            orderNumber = "ORD-" + datePart + "-" + suffix;
        } while (orderRepository.findByOrderNumber(orderNumber).isPresent());
        return orderNumber;
    }
}
