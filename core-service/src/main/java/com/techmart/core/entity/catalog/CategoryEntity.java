package com.techmart.core.entity.catalog;

import com.techmart.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class CategoryEntity extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CategoryEntity parent;

    @Column(name = "level", nullable = false)
    private int level = 1;

    @Column(name = "path", nullable = false, length = 500)
    private String path;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
