package com.techmart.core.repository.cart;

import com.techmart.core.entity.cart.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {

    Optional<CartItemEntity> findByCartIdAndVariantId(UUID cartId, UUID variantId);

    List<CartItemEntity> findByCartId(UUID cartId);

    @Query("SELECT ci FROM CartItemEntity ci JOIN FETCH ci.variant v JOIN FETCH v.product p WHERE ci.cart.id = :cartId ORDER BY ci.createdAt ASC")
    List<CartItemEntity> findByCartIdWithVariantAndProduct(@Param("cartId") UUID cartId);

    @Modifying
    @Query("DELETE FROM CartItemEntity ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") UUID cartId);

    @Modifying
    @Query("DELETE FROM CartItemEntity ci WHERE ci.id = :id AND ci.cart.id = :cartId")
    int deleteByIdAndCartId(@Param("id") UUID id, @Param("cartId") UUID cartId);
}
