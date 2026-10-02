package com.medtrack.model;

public class Category {
    private int id;
    private String name;
    private String description;
    private String icon;
    private int medicineCount;
    private String createdAt;

    public Category() {}

    public Category(int id, String name, String description, String icon, int medicineCount, String createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.medicineCount = medicineCount;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public int getMedicineCount() { return medicineCount; }
    public void setMedicineCount(int medicineCount) { this.medicineCount = medicineCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
