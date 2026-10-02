package com.medtrack.controller;

import com.google.gson.JsonObject;
import com.medtrack.dao.AuditLogDAO;
import com.medtrack.dao.BatchDAO;
import com.medtrack.dao.PurchaseOrderDAO;
import com.medtrack.model.Batch;
import com.medtrack.model.PurchaseOrder;
import com.medtrack.model.PurchaseOrderItem;
import com.medtrack.model.User;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;
import com.medtrack.service.AuthService;
import com.medtrack.util.DateUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PurchaseOrderController {
    private final PurchaseOrderDAO poDAO = new PurchaseOrderDAO();
    private final BatchDAO batchDAO = new BatchDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final AuthService authService;

    public PurchaseOrderController(AuthService authService) {
        this.authService = authService;
    }

    public void handleGetAll(RequestContext ctx) throws IOException {
        String status = ctx.getQueryParam("status");
        List<PurchaseOrder> list = poDAO.findAll(status);
        JsonResponse.ok(ctx.getExchange(), list);
    }

    public void handleGetById(RequestContext ctx, int id) throws IOException {
        PurchaseOrder po = poDAO.findById(id);
        if (po == null) {
            JsonResponse.notFound(ctx.getExchange(), "Purchase order not found.");
            return;
        }
        JsonResponse.ok(ctx.getExchange(), po);
    }

    public void handleCreate(RequestContext ctx) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN", "INVENTORY_MANAGER")) {
            JsonResponse.forbidden(ctx.getExchange(), "Permission denied to create purchase orders.");
            return;
        }

        PurchaseOrder po = ctx.getBodyAs(PurchaseOrder.class);
        if (po == null || po.getItems() == null || po.getItems().isEmpty()) {
            JsonResponse.badRequest(ctx.getExchange(), "Purchase order requires a supplier and line items.");
            return;
        }

        po.setCreatedBy(user.getId());

        try {
            PurchaseOrder created = poDAO.create(po);
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "CREATE_PO", "PURCHASE_ORDER", created.getPoNumber(), "Created PO #" + created.getPoNumber() + " with " + created.getItems().size() + " items (Total: $" + String.format("%.2f", created.getTotalAmount()) + ")", ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Purchase order created successfully.", created);
        } catch (SQLException e) {
            JsonResponse.error(ctx.getExchange(), "Failed to create purchase order: " + e.getMessage());
        }
    }

    public void handleUpdateStatus(RequestContext ctx, int id) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN", "INVENTORY_MANAGER")) {
            JsonResponse.forbidden(ctx.getExchange(), "Permission denied.");
            return;
        }

        JsonObject body = ctx.getBodyAsJsonObject();
        String status = body.get("status").getAsString().toUpperCase();

        PurchaseOrder po = poDAO.findById(id);
        if (po == null) {
            JsonResponse.notFound(ctx.getExchange(), "PO not found.");
            return;
        }

        boolean ok = poDAO.updateStatus(id, status);
        if (ok) {
            // If marking RECEIVED, automatically create stock batches in the inventory!
            if ("RECEIVED".equalsIgnoreCase(status) && !"RECEIVED".equalsIgnoreCase(po.getStatus())) {
                LocalDate today = LocalDate.now();
                for (PurchaseOrderItem item : po.getItems()) {
                    Batch newBatch = new Batch();
                    newBatch.setMedicineId(item.getMedicineId());
                    newBatch.setBatchNumber("PO-" + po.getId() + "-" + item.getMedicineId() + "-" + System.currentTimeMillis() % 1000);
                    newBatch.setMfgDate(today.minusMonths(1).format(DateUtils.DATE_FORMATTER));
                    newBatch.setExpiryDate(today.plusMonths(18).format(DateUtils.DATE_FORMATTER)); // Default safe 18 months
                    newBatch.setQuantity(item.getQuantity());
                    newBatch.setOriginalQuantity(item.getQuantity());
                    newBatch.setUnitCost(item.getUnitCost());
                    newBatch.setUnitPrice(item.getUnitCost() * 1.6); // Default 60% margin
                    newBatch.setDiscountPercent(0.0);
                    newBatch.setShelfLocation("Receiving Bay / Shelf Main");
                    newBatch.setSupplierId(po.getSupplierId());
                    newBatch.setStatus("ACTIVE");
                    newBatch.setNotes("Auto-ingested from received PO #" + po.getPoNumber());
                    batchDAO.create(newBatch);
                }
            }

            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "UPDATE_PO_STATUS", "PURCHASE_ORDER", po.getPoNumber(), "Changed PO status to " + status, ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Purchase order status updated to " + status + ".", null);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to update PO status.");
        }
    }
}
