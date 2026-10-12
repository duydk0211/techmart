package com.techmart.core.repository.user;

import com.techmart.core.entity.user.Role;
import com.techmart.core.entity.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

    boolean existsByEmailAndDeletedFalse(String email);

    Page<UserEntity> findByRoleAndDeletedFalse(Role role, Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.role = 'CUSTOMER' AND u.deleted = false " +
           "AND (:keyword IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<UserEntity> searchCustomers(@Param("keyword") String keyword, Pageable pageable);

    Optional<UserEntity> findByIdAndDeletedFalse(UUID id);
}
