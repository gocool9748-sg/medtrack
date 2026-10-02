package com.medtrack.model;

public class Medicine {
    private int id;
    private String name;
    private String genericName;
    private int categoryId;
    private String categoryName;
    private String dosageForm; // Tablet, Capsule, Syrup, Injection, Inhaler, Drops, Ointment
    private String strength;   // e.g. 500mg, 100ml
    private String unit;       // Strip, Bottle, Box, Vial, Tube
    private int reorderLevel;
    private int minAlertDays;  // Expiry alert threshold in days (e.g. 60)
    private boolean requiresPrescription;
    private String storageCondition; // Room Temperature (15-25°C), Refrigerated (2-8°C), Cool & Dry
    private String sideEffects;
    private String barcode;
    private boolean isActive;
    private String createdAt;
    
    // Calculated runtime fields
    private int totalStock;
    private int activeBatches;
    private double minPrice;
    private double maxPrice;

    public Medicine() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }

    public int getMinAlertDays() { return minAlertDays; }
    public void setMinAlertDays(int minAlertDays) { this.minAlertDays = minAlertDays; }

    public boolean isRequiresPrescription() { return requiresPrescription; }
    public void setRequiresPrescription(boolean requiresPrescription) { this.requiresPrescription = requiresPrescription; }

    public String getStorageCondition() { return storageCondition; }
    public void setStorageCondition(String storageCondition) { this.storageCondition = storageCondition; }

    public String getSideEffects() { return sideEffects; }
    public void setSideEffects(String sideEffects) { this.sideEffects = sideEffects; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getTotalStock() { return totalStock; }
    public void setTotalStock(int totalStock) { this.totalStock = totalStock; }

    public int getActiveBatches() { return activeBatches; }
    public void setActiveBatches(int activeBatches) { this.activeBatches = activeBatches; }

    public double getMinPrice() { return minPrice; }
    public void setMinPrice(double minPrice) { this.minPrice = minPrice; }

    public double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(double maxPrice) { this.maxPrice = maxPrice; }
}
