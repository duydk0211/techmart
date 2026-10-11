package com.techmart.core.repository.catalog;

import com.techmart.core.entity.catalog.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, UUID> {

    boolean existsBySku(String sku);

    Optional<ProductVariantEntity> findBySku(String sku);

    List<ProductVariantEntity> findByProductIdAndStatus(UUID productId, String status);

    List<ProductVariantEntity> findByProductId(UUID productId);
}
