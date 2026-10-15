package com.techmart.core.repository.catalog;

import com.techmart.core.entity.catalog.CategoryEntity;
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
public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    Optional<CategoryEntity> findByParentIsNullAndNameIgnoreCase(String name);

    Optional<CategoryEntity> findByParentIdAndNameIgnoreCase(UUID parentId, String name);

    List<CategoryEntity> findAllByActiveTrueOrderByLevelAscNameAsc();

    @Query("SELECT COALESCE(MAX(c.level), 0) FROM CategoryEntity c WHERE c.path LIKE CONCAT(:pathPrefix, '/%')")
    int findMaxDescendantLevel(@Param("pathPrefix") String pathPrefix);

    @Query("SELECT c FROM CategoryEntity c WHERE c.path LIKE CONCAT(:pathPrefix, '/%')")
    List<CategoryEntity> findDescendants(@Param("pathPrefix") String pathPrefix);

    @Modifying
    @Query("UPDATE CategoryEntity c SET c.active = false, c.updatedAt = :now WHERE c.path LIKE CONCAT(:pathPrefix, '/%')")
    void deactivateSubtree(@Param("pathPrefix") String pathPrefix, @Param("now") Instant now);
}
