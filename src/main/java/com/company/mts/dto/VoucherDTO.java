package com.company.mts.dto;

import java.math.BigDecimal;

public class VoucherDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private int pointCost;
    private BigDecimal cashValue;
    private String category;
    private String icon;
    private boolean active;
    private int stock;
    private int effectiveCost;
    private boolean canAfford;

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
    public int getEffectiveCost() { return effectiveCost; }
    public void setEffectiveCost(int effectiveCost) { this.effectiveCost = effectiveCost; }
    public boolean isCanAfford() { return canAfford; }
    public void setCanAfford(boolean canAfford) { this.canAfford = canAfford; }
}
