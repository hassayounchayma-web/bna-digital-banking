package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "credit_tmm")
public class TmmEntity {
    @Id
    private Long id;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal valuePercent;

    @Column(nullable = false)
    private LocalDateTime modifiedAt;

    protected TmmEntity() {
    }

    public TmmEntity(Long id, BigDecimal valuePercent) {
        this.id = id;
        this.valuePercent = valuePercent;
    }

    @PrePersist
    @PreUpdate
    public void updateModifiedAt() {
        modifiedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getValuePercent() {
        return valuePercent;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void updateValue(BigDecimal valuePercent) {
        this.valuePercent = valuePercent;
    }
}
