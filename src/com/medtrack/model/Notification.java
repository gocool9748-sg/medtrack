package com.medtrack.model;

public class Notification {
    private int id;
    private String title;
    private String message;
    private String type; // EXPIRY, LOW_STOCK, PO_UPDATE, SYSTEM, SECURITY
    private String severity; // INFO, WARNING, CRITICAL, DANGER
    private boolean isRead;
    private String linkType; // BATCH, MEDICINE, PO, DISPOSAL
    private String linkId;
    private String createdAt;

    public Notification() {}

    public Notification(int id, String title, String message, String type, String severity, boolean isRead, String linkType, String linkId, String createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.severity = severity;
        this.isRead = isRead;
        this.linkType = linkType;
        this.linkId = linkId;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getLinkType() { return linkType; }
    public void setLinkType(String linkType) { this.linkType = linkType; }

    public String getLinkId() { return linkId; }
    public void setLinkId(String linkId) { this.linkId = linkId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
