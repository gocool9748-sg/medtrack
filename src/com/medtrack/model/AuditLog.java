package com.medtrack.model;

public class AuditLog {
    private int id;
    private int userId;
    private String username;
    private String role;
    private String action; // LOGIN, LOGOUT, CREATE_MEDICINE, UPDATE_STOCK, DISPENSE_SALE, QUARANTINE_BATCH, DISPOSE_BATCH, CREATE_PO, SYSTEM_BACKUP
    private String entityType; // MEDICINE, BATCH, SALE, SUPPLIER, DISPOSAL, USER, SYSTEM
    private String entityId;
    private String details;
    private String ipAddress;
    private String createdAt;

    public AuditLog() {}

    public AuditLog(int id, int userId, String username, String role, String action, String entityType, String entityId, String details, String ipAddress, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
