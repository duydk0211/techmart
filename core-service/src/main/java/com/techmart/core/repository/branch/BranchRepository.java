package com.techmart.core.repository.branch;

import com.techmart.core.entity.branch.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);

    Optional<BranchEntity> findByNameIgnoreCase(String name);

    List<BranchEntity> findAllByActiveTrueOrderByNameAsc();
}
