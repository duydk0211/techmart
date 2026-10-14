package com.techmart.core.repository.catalog;

import com.techmart.core.entity.catalog.BannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BannerRepository extends JpaRepository<BannerEntity, UUID> {
    List<BannerEntity> findAllByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
}
