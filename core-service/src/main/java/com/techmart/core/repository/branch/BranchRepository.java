package com.techmart.core.repository.branch;

import com.techmart.core.entity.branch.BranchEntity;
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
public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);

    Optional<BranchEntity> findByNameIgnoreCase(String name);

    List<BranchEntity> findAllByActiveTrueOrderByNameAsc();

    @Modifying
    @Query("UPDATE BranchEntity b SET b.quantityAvailable = b.quantityAvailable - :qty, b.updatedAt = :now WHERE b.id = :branchId AND b.quantityAvailable >= :qty")
    int deductAvailableStock(@Param("branchId") UUID branchId, @Param("qty") int qty, @Param("now") Instant now);
}
