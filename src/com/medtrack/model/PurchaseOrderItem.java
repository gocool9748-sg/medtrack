package com.medtrack.model;

public class PurchaseOrderItem {
    private int id;
    private int poId;
    private int medicineId;
    private String medicineName;
    private String genericName;
    private int quantity;
    private double unitCost;
    private double subtotal;

    public PurchaseOrderItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPoId() { return poId; }
    public void setPoId(int poId) { this.poId = poId; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitCost() { return unitCost; }
    public void setUnitCost(double unitCost) { this.unitCost = unitCost; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
}
