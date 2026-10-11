package com.techmart.core.repository.catalog;

import com.techmart.core.dto.response.catalog.ProductSummaryProjectionRow;
import com.techmart.core.entity.catalog.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    boolean existsBySlug(String slug);

    Optional<ProductEntity> findBySlug(String slug);

    @Query(
            value = """
                SELECT new com.techmart.core.dto.response.catalog.ProductSummaryProjectionRow(
                    p.id,
                    p.name,
                    p.slug,
                    c.id,
                    c.name,
                    b.id,
                    b.name,
                    p.thumbnailUrl,
                    MIN(v.price),
                    MAX(v.price),
                    COUNT(DISTINCT v.id),
                    SUM(CASE WHEN (inv.quantityOnHand - inv.quantityReserved) > 0 THEN 1L ELSE 0L END),
                    p.status
                )
                FROM ProductEntity p
                JOIN p.brand b
                JOIN p.category c
                LEFT JOIN ProductVariantEntity v ON v.product.id = p.id AND v.status = 'ACTIVE'
                LEFT JOIN InventoryEntity inv ON inv.variant.id = v.id
                WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))
                  AND (:categoryPath IS NULL OR c.path LIKE CONCAT(:categoryPath, '%'))
                  AND (:brandId IS NULL OR b.id = :brandId)
                  AND (:branchId IS NULL OR inv.branch.id = :branchId)
                  AND p.status = 'ACTIVE'
                GROUP BY p.id, p.name, p.slug, c.id, c.name, b.id, b.name, p.thumbnailUrl, p.status
                HAVING (:minPrice IS NULL OR MIN(v.price) >= :minPrice)
                   AND (:maxPrice IS NULL OR MAX(v.price) <= :maxPrice)
                   AND (:inStockOnly = false OR SUM(CASE WHEN (inv.quantityOnHand - inv.quantityReserved) > 0 THEN 1L ELSE 0L END) > 0)
            """,
            countQuery = """
                SELECT COUNT(DISTINCT p.id)
                FROM ProductEntity p
                JOIN p.brand b
                JOIN p.category c
                LEFT JOIN ProductVariantEntity v ON v.product.id = p.id AND v.status = 'ACTIVE'
                LEFT JOIN InventoryEntity inv ON inv.variant.id = v.id
                WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))
                  AND (:categoryPath IS NULL OR c.path LIKE CONCAT(:categoryPath, '%'))
                  AND (:brandId IS NULL OR b.id = :brandId)
                  AND (:branchId IS NULL OR inv.branch.id = :branchId)
                  AND p.status = 'ACTIVE'
            """
    )
    Page<ProductSummaryProjectionRow> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryPath") String categoryPath,
            @Param("brandId") UUID brandId,
            @Param("branchId") UUID branchId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("inStockOnly") boolean inStockOnly,
            Pageable pageable
    );
}
