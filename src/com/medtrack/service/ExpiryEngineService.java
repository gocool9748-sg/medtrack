package com.medtrack.service;

import com.medtrack.dao.BatchDAO;
import com.medtrack.dao.NotificationDAO;
import com.medtrack.model.Batch;
import com.medtrack.model.Notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpiryEngineService {
    private final BatchDAO batchDAO = new BatchDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    public static class MarkdownRecommendation {
        public int batchId;
        public String batchNumber;
        public String medicineName;
        public long daysToExpiry;
        public double currentPrice;
        public double recommendedDiscountPercent;
        public double recommendedSalePrice;
        public String strategy; // CLEARANCE_FLASH, PROMOTIONAL_MARKDOWN, SUPPLIER_RETURN, QUARANTINE_WARNING
        public String justification;
    }

    public List<MarkdownRecommendation> generateMarkdownRecommendations() {
        List<MarkdownRecommendation> recs = new ArrayList<>();
        List<Batch> batches = batchDAO.findAll(null, null, null, null);

        for (Batch b : batches) {
            if (b.getQuantity() <= 0 || "QUARANTINED".equalsIgnoreCase(b.getStatus()) || "RECALLED".equalsIgnoreCase(b.getStatus())) {
                continue;
            }

            long days = b.getDaysToExpiry();
            if (days < 0) {
                MarkdownRecommendation rec = new MarkdownRecommendation();
                rec.batchId = b.getId();
                rec.batchNumber = b.getBatchNumber();
                rec.medicineName = b.getMedicineName();
                rec.daysToExpiry = days;
                rec.currentPrice = b.getUnitPrice();
                rec.recommendedDiscountPercent = 0.0;
                rec.recommendedSalePrice = 0.0;
                rec.strategy = "QUARANTINE_FOR_DISPOSAL";
                rec.justification = "Batch expired " + Math.abs(days) + " days ago. Immediate quarantine and safe destruction required.";
                recs.add(rec);
            } else if (days <= 15) {
                MarkdownRecommendation rec = new MarkdownRecommendation();
                rec.batchId = b.getId();
                rec.batchNumber = b.getBatchNumber();
                rec.medicineName = b.getMedicineName();
                rec.daysToExpiry = days;
                rec.currentPrice = b.getUnitPrice();
                rec.recommendedDiscountPercent = 40.0;
                rec.recommendedSalePrice = b.getUnitPrice() * 0.60;
                rec.strategy = "CLEARANCE_FLASH";
                rec.justification = "Expires in " + days + " days. High waste risk! Apply 40% clearance discount or return to vendor.";
                recs.add(rec);
            } else if (days <= 45) {
                MarkdownRecommendation rec = new MarkdownRecommendation();
                rec.batchId = b.getId();
                rec.batchNumber = b.getBatchNumber();
                rec.medicineName = b.getMedicineName();
                rec.daysToExpiry = days;
                rec.currentPrice = b.getUnitPrice();
                rec.recommendedDiscountPercent = 25.0;
                rec.recommendedSalePrice = b.getUnitPrice() * 0.75;
                rec.strategy = "PROMOTIONAL_MARKDOWN";
                rec.justification = "Expires in " + days + " days. Apply 25% promotional markdown to accelerate FEFO dispensing.";
                recs.add(rec);
            } else if (days <= 90) {
                MarkdownRecommendation rec = new MarkdownRecommendation();
                rec.batchId = b.getId();
                rec.batchNumber = b.getBatchNumber();
                rec.medicineName = b.getMedicineName();
                rec.daysToExpiry = days;
                rec.currentPrice = b.getUnitPrice();
                rec.recommendedDiscountPercent = 15.0;
                rec.recommendedSalePrice = b.getUnitPrice() * 0.85;
                rec.strategy = "EARLY_DISCOUNT";
                rec.justification = "Expires in " + days + " days. Consider 15% discount or prioritize in doctor prescription queue.";
                recs.add(rec);
            }
        }
        return recs;
    }

    public Map<String, Object> runAutomatedExpiryScan() {
        List<Batch> batches = batchDAO.findAll(null, null, null, null);
        int expiredCount = 0;
        int criticalCount = 0;
        int warningCount = 0;
        int notificationsCreated = 0;

        for (Batch b : batches) {
            long days = b.getDaysToExpiry();
            if (days < 0 && !"EXPIRED".equalsIgnoreCase(b.getStatus()) && !"QUARANTINED".equalsIgnoreCase(b.getStatus())) {
                expiredCount++;
                notificationDAO.create(new Notification(
                    0,
                    "EXPIRED BATCH: " + b.getMedicineName() + " (" + b.getBatchNumber() + ")",
                    "Batch has expired on " + b.getExpiryDate() + ". Please quarantine immediately.",
                    "EXPIRY",
                    "DANGER",
                    false,
                    "BATCH",
                    String.valueOf(b.getId()),
                    null
                ));
                notificationsCreated++;
            } else if (days >= 0 && days <= 30 && !"EXPIRING_SOON".equalsIgnoreCase(b.getStatus())) {
                criticalCount++;
                notificationDAO.create(new Notification(
                    0,
                    "CRITICAL EXPIRY: " + b.getMedicineName() + " (" + days + " days left)",
                    "Batch " + b.getBatchNumber() + " expires in " + days + " days. Stock: " + b.getQuantity() + " units.",
                    "EXPIRY",
                    "CRITICAL",
                    false,
                    "BATCH",
                    String.valueOf(b.getId()),
                    null
                ));
                notificationsCreated++;
            } else if (days > 30 && days <= 90) {
                warningCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("scannedBatches", batches.size());
        result.put("expiredCount", expiredCount);
        result.put("criticalCount", criticalCount);
        result.put("warningCount", warningCount);
        result.put("notificationsCreated", notificationsCreated);
        result.put("message", "Automated expiry sweep complete. Found " + criticalCount + " critical and " + expiredCount + " expired batches.");
        return result;
    }
}
