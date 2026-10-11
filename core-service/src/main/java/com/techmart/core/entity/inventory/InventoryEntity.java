package com.techmart.core.entity.inventory;

import com.techmart.core.entity.branch.BranchEntity;
import com.techmart.core.entity.catalog.ProductVariantEntity;
import com.techmart.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "inventory",
        uniqueConstraints = @UniqueConstraint(name = "uq_inventory_branch_variant", columnNames = {"branch_id", "variant_id"})
)
public class InventoryEntity extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariantEntity variant;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand = 0;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved = 0;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel = 5;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0L;
}
