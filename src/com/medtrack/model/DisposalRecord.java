package com.medtrack.model;

public class DisposalRecord {
    private int id;
    private int batchId;
    private int medicineId;
    private String medicineName;
    private String genericName;
    private String batchNumber;
    private int quantity;
    private double unitCost;
    private double totalLoss;
    private String reason; // EXPIRED, DAMAGED, CONTAMINATED, RECALLED, COLD_CHAIN_FAILURE
    private String destructionMethod; // INCINERATION, ENCAPSULATION, RETURN_TO_VENDOR, CHEMICAL_INACTIVATION
    private int authorizedBy;
    private String authorizerName;
    private String disposalDate;
    private String certificateNumber;
    private String notes;

    public DisposalRecord() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitCost() { return unitCost; }
    public void setUnitCost(double unitCost) { this.unitCost = unitCost; }

    public double getTotalLoss() { return totalLoss; }
    public void setTotalLoss(double totalLoss) { this.totalLoss = totalLoss; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDestructionMethod() { return destructionMethod; }
    public void setDestructionMethod(String destructionMethod) { this.destructionMethod = destructionMethod; }

    public int getAuthorizedBy() { return authorizedBy; }
    public void setAuthorizedBy(int authorizedBy) { this.authorizedBy = authorizedBy; }

    public String getAuthorizerName() { return authorizerName; }
    public void setAuthorizerName(String authorizerName) { this.authorizerName = authorizerName; }

    public String getDisposalDate() { return disposalDate; }
    public void setDisposalDate(String disposalDate) { this.disposalDate = disposalDate; }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
