package com.techmart.core.entity.cart;

import com.techmart.core.entity.catalog.ProductVariantEntity;
import com.techmart.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cart_items_cart_variant", columnNames = {"cart_id", "variant_id"})
})
public class CartItemEntity extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private CartEntity cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariantEntity variant;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    public CartItemEntity(CartEntity cart, ProductVariantEntity variant, int quantity) {
        this.cart = cart;
        this.variant = variant;
        this.quantity = quantity;
    }
}
