package com.medtrack.controller;

import com.google.gson.JsonObject;
import com.medtrack.dao.AuditLogDAO;
import com.medtrack.dao.BatchDAO;
import com.medtrack.model.Batch;
import com.medtrack.model.User;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;
import com.medtrack.service.AuthService;

import java.io.IOException;
import java.util.List;

public class BatchController {
    private final BatchDAO batchDAO = new BatchDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final AuthService authService;

    public BatchController(AuthService authService) {
        this.authService = authService;
    }

    public void handleGetAll(RequestContext ctx) throws IOException {
        String search = ctx.getQueryParam("search");
        String status = ctx.getQueryParam("status");
        Integer medicineId = ctx.getQueryParamAsInt("medicineId");
        Integer days = ctx.getQueryParamAsInt("days");

        List<Batch> list = batchDAO.findAll(search, status, medicineId, days);
        JsonResponse.ok(ctx.getExchange(), list);
    }

    public void handleGetFEFO(RequestContext ctx, int medicineId) throws IOException {
        List<Batch> list = batchDAO.findFEFOBatchesForMedicine(medicineId);
        JsonResponse.ok(ctx.getExchange(), list);
    }

    public void handleGetById(RequestContext ctx, int id) throws IOException {
        Batch b = batchDAO.findById(id);
        if (b == null) {
            JsonResponse.notFound(ctx.getExchange(), "Batch with ID " + id + " not found.");
            return;
        }
        JsonResponse.ok(ctx.getExchange(), b);
    }

    public void handleCreate(RequestContext ctx) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN", "INVENTORY_MANAGER", "PHARMACIST")) {
            JsonResponse.forbidden(ctx.getExchange(), "Permission denied to add stock batches.");
            return;
        }

        Batch b = ctx.getBodyAs(Batch.class);
        if (b == null || b.getBatchNumber() == null || b.getBatchNumber().isBlank()) {
            JsonResponse.badRequest(ctx.getExchange(), "Batch number and medicine ID are required.");
            return;
        }

        boolean ok = batchDAO.create(b);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "CREATE_BATCH", "BATCH", String.valueOf(b.getId()), "Created batch " + b.getBatchNumber() + " with " + b.getQuantity() + " units (Exp: " + b.getExpiryDate() + ")", ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Stock batch registered successfully.", b);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to create batch. Verify that batch number is unique.");
        }
    }

    public void handleUpdate(RequestContext ctx, int id) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN", "INVENTORY_MANAGER")) {
            JsonResponse.forbidden(ctx.getExchange(), "Permission denied to update batch.");
            return;
        }

        Batch b = ctx.getBodyAs(Batch.class);
        if (b == null) {
            JsonResponse.badRequest(ctx.getExchange(), "Invalid batch payload.");
            return;
        }
        b.setId(id);

        boolean ok = batchDAO.update(b);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "UPDATE_BATCH", "BATCH", String.valueOf(id), "Updated batch details for: " + b.getBatchNumber(), ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Batch updated successfully.", b);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to update batch.");
        }
    }

    public void handleQuarantine(RequestContext ctx, int id) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }

        JsonObject body = ctx.getBodyAsJsonObject();
        String reason = body.has("reason") ? body.get("reason").getAsString() : "Manual quarantine by pharmacist";

        boolean ok = batchDAO.quarantineBatch(id, reason);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "QUARANTINE_BATCH", "BATCH", String.valueOf(id), "Quarantined batch ID: " + id + " Reason: " + reason, ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Batch has been quarantined and locked from active sales dispensing.", null);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to quarantine batch.");
        }
    }

    public void handleDiscount(RequestContext ctx, int id) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }

        JsonObject body = ctx.getBodyAsJsonObject();
        double discount = body.has("discountPercent") ? body.get("discountPercent").getAsDouble() : 0.0;

        boolean ok = batchDAO.updateDiscount(id, discount);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "DISCOUNT_BATCH", "BATCH", String.valueOf(id), "Applied " + discount + "% markdown to batch ID: " + id, ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Markdown discount of " + discount + "% applied to batch.", null);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to apply discount.");
        }
    }

    public void handleStockAdjustment(RequestContext ctx, int id) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN", "INVENTORY_MANAGER", "AUDITOR")) {
            JsonResponse.forbidden(ctx.getExchange(), "Permission denied for physical stock adjustments.");
            return;
        }

        JsonObject body = ctx.getBodyAsJsonObject();
        int newQty = body.get("quantity").getAsInt();
        String reason = body.has("reason") ? body.get("reason").getAsString() : "Physical count verification";

        boolean ok = batchDAO.adjustStock(id, newQty, reason);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "ADJUST_STOCK", "BATCH", String.valueOf(id), "Adjusted batch ID " + id + " quantity to " + newQty + " (" + reason + ")", ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Stock count updated successfully.", null);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to adjust stock.");
        }
    }

    public void handleDelete(RequestContext ctx, int id) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN")) {
            JsonResponse.forbidden(ctx.getExchange(), "Only administrators can delete batch records.");
            return;
        }

        boolean ok = batchDAO.delete(id);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "DELETE_BATCH", "BATCH", String.valueOf(id), "Deleted batch record ID: " + id, ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Batch record deleted.", null);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to delete batch.");
        }
    }
}
