package com.techmart.core.repository.inventory;

import com.techmart.core.entity.inventory.InventoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, UUID> {

    Optional<InventoryEntity> findByBranchIdAndVariantId(UUID branchId, UUID variantId);

    List<InventoryEntity> findByVariantId(UUID variantId);

    @Query(
            value = "SELECT i FROM InventoryEntity i JOIN FETCH i.variant v JOIN FETCH v.product p WHERE i.branch.id = :branchId",
            countQuery = "SELECT COUNT(i) FROM InventoryEntity i WHERE i.branch.id = :branchId"
    )
    Page<InventoryEntity> findByBranchIdWithVariantAndProduct(@Param("branchId") UUID branchId, Pageable pageable);

    @Modifying
    @Query("UPDATE InventoryEntity i SET i.quantityReserved = i.quantityReserved + :requestedQty, i.version = i.version + 1, i.updatedAt = :now WHERE i.branch.id = :branchId AND i.variant.id = :variantId AND (i.quantityOnHand - i.quantityReserved) >= :requestedQty")
    int reserveStock(@Param("branchId") UUID branchId, @Param("variantId") UUID variantId, @Param("requestedQty") int requestedQty, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE InventoryEntity i SET i.quantityReserved = i.quantityReserved - :qty, i.version = i.version + 1, i.updatedAt = :now WHERE i.branch.id = :branchId AND i.variant.id = :variantId AND i.quantityReserved >= :qty")
    int releaseStock(@Param("branchId") UUID branchId, @Param("variantId") UUID variantId, @Param("qty") int qty, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE InventoryEntity i SET i.quantityOnHand = i.quantityOnHand - :qty, i.quantityReserved = i.quantityReserved - :qty, i.version = i.version + 1, i.updatedAt = :now WHERE i.branch.id = :branchId AND i.variant.id = :variantId AND i.quantityOnHand >= :qty AND i.quantityReserved >= :qty")
    int deductStockOnDelivery(@Param("branchId") UUID branchId, @Param("variantId") UUID variantId, @Param("qty") int qty, @Param("now") Instant now);
}
