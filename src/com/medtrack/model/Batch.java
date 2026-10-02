package com.medtrack.model;

import com.medtrack.util.DateUtils;

public class Batch {
    private int id;
    private int medicineId;
    private String medicineName;
    private String genericName;
    private String dosageForm;
    private String strength;
    private String categoryName;
    private String batchNumber;
    private String mfgDate;
    private String expiryDate;
    private int quantity;
    private int originalQuantity;
    private double unitCost;
    private double unitPrice;
    private double discountPercent;
    private String shelfLocation; // e.g. Shelf A-3, Rack 2, Cold Storage 1
    private int supplierId;
    private String supplierName;
    private String status; // ACTIVE, EXPIRING_SOON, EXPIRED, QUARANTINED, RECALLED
    private String notes;
    private String createdAt;

    // Runtime calculated fields
    private long daysToExpiry;
    private String expiryStatus; // SAFE, WARNING, CRITICAL, EXPIRED
    private double effectivePrice;

    public Batch() {}

    public void calculateExpiryMetrics() {
        this.daysToExpiry = DateUtils.daysUntil(this.expiryDate);
        if ("QUARANTINED".equalsIgnoreCase(this.status)) {
            this.expiryStatus = "QUARANTINED";
        } else if ("RECALLED".equalsIgnoreCase(this.status)) {
            this.expiryStatus = "RECALLED";
        } else {
            this.expiryStatus = DateUtils.getExpiryStatus(this.daysToExpiry);
            if (this.daysToExpiry < 0) {
                this.status = "EXPIRED";
            } else if (this.daysToExpiry <= 30 && !"QUARANTINED".equalsIgnoreCase(this.status)) {
                this.status = "EXPIRING_SOON";
            }
        }
        this.effectivePrice = this.unitPrice * (1.0 - (this.discountPercent / 100.0));
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getMfgDate() { return mfgDate; }
    public void setMfgDate(String mfgDate) { this.mfgDate = mfgDate; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getOriginalQuantity() { return originalQuantity; }
    public void setOriginalQuantity(int originalQuantity) { this.originalQuantity = originalQuantity; }

    public double getUnitCost() { return unitCost; }
    public void setUnitCost(double unitCost) { this.unitCost = unitCost; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }

    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public long getDaysToExpiry() { return daysToExpiry; }
    public void setDaysToExpiry(long daysToExpiry) { this.daysToExpiry = daysToExpiry; }

    public String getExpiryStatus() { return expiryStatus; }
    public void setExpiryStatus(String expiryStatus) { this.expiryStatus = expiryStatus; }

    public double getEffectivePrice() { return effectivePrice; }
    public void setEffectivePrice(double effectivePrice) { this.effectivePrice = effectivePrice; }
}
