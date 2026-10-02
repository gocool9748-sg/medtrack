package com.medtrack.model;

import java.util.ArrayList;
import java.util.List;

public class PurchaseOrder {
    private int id;
    private String poNumber;
    private int supplierId;
    private String supplierName;
    private String orderDate;
    private String expectedDate;
    private String receivedDate;
    private double totalAmount;
    private String status; // DRAFT, SENT, RECEIVED, CANCELLED
    private int createdBy;
    private String creatorName;
    private String notes;
    private List<PurchaseOrderItem> items = new ArrayList<>();

    public PurchaseOrder() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public String getExpectedDate() { return expectedDate; }
    public void setExpectedDate(String expectedDate) { this.expectedDate = expectedDate; }

    public String getReceivedDate() { return receivedDate; }
    public void setReceivedDate(String receivedDate) { this.receivedDate = receivedDate; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<PurchaseOrderItem> getItems() { return items; }
    public void setItems(List<PurchaseOrderItem> items) { this.items = items; }
}
