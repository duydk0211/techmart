package com.techmart.core.repository.catalog;

import com.techmart.core.entity.catalog.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<BrandEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);

    Optional<BrandEntity> findByNameIgnoreCase(String name);

    List<BrandEntity> findAllByActiveTrueOrderByNameAsc();
}
