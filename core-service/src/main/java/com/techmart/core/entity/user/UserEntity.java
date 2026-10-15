package com.techmart.core.entity.user;

import com.techmart.shared.audit.AuditableEntity;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity extends AuditableEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "user_address_info", nullable = false, columnDefinition = "jsonb")
    private List<AddressItem> userAddressInfo = new ArrayList<>();

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    public boolean isAccountLocked() {
        return this.lockedUntil != null && this.lockedUntil.isAfter(Instant.now());
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = Instant.now().plus(15, ChronoUnit.MINUTES);
        }
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void addAddress(AddressItem newItem) {
        if (this.userAddressInfo == null) {
            this.userAddressInfo = new ArrayList<>();
        }
        if (this.userAddressInfo.size() >= 10) {
            throw new CoreException(ResponseCode.ADDRESS_LIMIT_EXCEEDED);
        }

        boolean willBeDefault = newItem.isDefault() || this.userAddressInfo.isEmpty();
        List<AddressItem> updatedList = new ArrayList<>();

        for (AddressItem item : this.userAddressInfo) {
            if (willBeDefault && item.isDefault()) {
                updatedList.add(new AddressItem(item.id(), item.name(), item.phone(), item.street(), item.province(), item.city(), false));
            } else {
                updatedList.add(item);
            }
        }

        updatedList.add(new AddressItem(newItem.id(), newItem.name(), newItem.phone(), newItem.street(), newItem.province(), newItem.city(), willBeDefault));
        this.userAddressInfo = updatedList;
    }

    public void updateAddress(UUID addressId, AddressItem updatedData) {
        if (this.userAddressInfo == null) {
            throw new CoreException(ResponseCode.ADDRESS_NOT_FOUND);
        }

        boolean found = false;
        List<AddressItem> updatedList = new ArrayList<>();

        for (AddressItem item : this.userAddressInfo) {
            if (item.id().equals(addressId)) {
                found = true;
                updatedList.add(new AddressItem(addressId, updatedData.name(), updatedData.phone(), updatedData.street(), updatedData.province(), updatedData.city(), updatedData.isDefault()));
            } else {
                if (updatedData.isDefault() && item.isDefault()) {
                    updatedList.add(new AddressItem(item.id(), item.name(), item.phone(), item.street(), item.province(), item.city(), false));
                } else {
                    updatedList.add(item);
                }
            }
        }

        if (!found) {
            throw new CoreException(ResponseCode.ADDRESS_NOT_FOUND);
        }
        this.userAddressInfo = updatedList;
    }

    public void deleteAddress(UUID addressId) {
        if (this.userAddressInfo == null) {
            throw new CoreException(ResponseCode.ADDRESS_NOT_FOUND);
        }

        boolean found = false;
        boolean wasDefault = false;
        List<AddressItem> updatedList = new ArrayList<>();

        for (AddressItem item : this.userAddressInfo) {
            if (item.id().equals(addressId)) {
                found = true;
                if (item.isDefault()) {
                    wasDefault = true;
                }
            } else {
                updatedList.add(item);
            }
        }

        if (!found) {
            throw new CoreException(ResponseCode.ADDRESS_NOT_FOUND);
        }

        if (wasDefault && !updatedList.isEmpty()) {
            AddressItem first = updatedList.get(0);
            updatedList.set(0, new AddressItem(first.id(), first.name(), first.phone(), first.street(), first.province(), first.city(), true));
        }

        this.userAddressInfo = updatedList;
    }

    public void setDefaultAddress(UUID addressId) {
        if (this.userAddressInfo == null) {
            throw new CoreException(ResponseCode.ADDRESS_NOT_FOUND);
        }

        boolean found = false;
        List<AddressItem> updatedList = new ArrayList<>();

        for (AddressItem item : this.userAddressInfo) {
            boolean isTarget = item.id().equals(addressId);
            if (isTarget) {
                found = true;
            }
            updatedList.add(new AddressItem(item.id(), item.name(), item.phone(), item.street(), item.province(), item.city(), isTarget));
        }

        if (!found) {
            throw new CoreException(ResponseCode.ADDRESS_NOT_FOUND);
        }
        this.userAddressInfo = updatedList;
    }
}
