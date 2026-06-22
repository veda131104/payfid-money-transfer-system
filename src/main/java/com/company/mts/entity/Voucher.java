package com.company.mts.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int pointCost;

    @Column(precision = 19, scale = 2)
    private BigDecimal cashValue;

    @Column(length = 50)
    private String category;

    @Column(length = 100)
    private String icon;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int stock = 999;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Voucher() {}

    public Voucher(String code, String name, String description, int pointCost, BigDecimal cashValue, String category, String icon) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.pointCost = pointCost;
        this.cashValue = cashValue;
        this.category = category;
        this.icon = icon;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getPointCost() { return pointCost; }
    public void setPointCost(int pointCost) { this.pointCost = pointCost; }
    public BigDecimal getCashValue() { return cashValue; }
    public void setCashValue(BigDecimal cashValue) { this.cashValue = cashValue; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
