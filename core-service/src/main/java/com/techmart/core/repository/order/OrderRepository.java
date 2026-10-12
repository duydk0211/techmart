package com.techmart.core.repository.order;

import com.techmart.core.entity.order.OrderEntity;
import com.techmart.core.entity.order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    Page<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    Page<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT DISTINCT o FROM OrderEntity o LEFT JOIN FETCH o.branch LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<OrderEntity> findByIdWithDetails(@Param("id") UUID id);
}
