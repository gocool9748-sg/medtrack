package com.medtrack.controller;

import com.medtrack.dao.AuditLogDAO;
import com.medtrack.dao.BatchDAO;
import com.medtrack.dao.MedicineDAO;
import com.medtrack.model.Batch;
import com.medtrack.model.Medicine;
import com.medtrack.model.User;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;
import com.medtrack.service.AuthService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicineController {
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final BatchDAO batchDAO = new BatchDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final AuthService authService;

    public MedicineController(AuthService authService) {
        this.authService = authService;
    }

    public void handleGetAll(RequestContext ctx) throws IOException {
        String search = ctx.getQueryParam("search");
        Integer categoryId = ctx.getQueryParamAsInt("categoryId");
        String stockStatus = ctx.getQueryParam("stockStatus");

        List<Medicine> medicines = medicineDAO.findAll(search, categoryId, stockStatus);
        JsonResponse.ok(ctx.getExchange(), medicines);
    }

    public void handleGetById(RequestContext ctx, int id) throws IOException {
        Medicine m = medicineDAO.findById(id);
        if (m == null) {
            JsonResponse.notFound(ctx.getExchange(), "Medicine with ID " + id + " not found.");
            return;
        }
        List<Batch> batches = batchDAO.findAll(null, null, id, null);
        Map<String, Object> res = new HashMap<>();
        res.put("medicine", m);
        res.put("batches", batches);
        JsonResponse.ok(ctx.getExchange(), res);
    }

    public void handleGetByBarcode(RequestContext ctx, String barcode) throws IOException {
        Medicine m = medicineDAO.findByBarcode(barcode);
        if (m == null) {
            JsonResponse.notFound(ctx.getExchange(), "No medicine found with barcode: " + barcode);
            return;
        }
        List<Batch> batches = batchDAO.findFEFOBatchesForMedicine(m.getId());
        Map<String, Object> res = new HashMap<>();
        res.put("medicine", m);
        res.put("batches", batches);
        JsonResponse.ok(ctx.getExchange(), res);
    }

    public void handleCreate(RequestContext ctx) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN", "INVENTORY_MANAGER", "PHARMACIST")) {
            JsonResponse.forbidden(ctx.getExchange(), "Permission denied to add medicines.");
            return;
        }

        Medicine m = ctx.getBodyAs(Medicine.class);
        if (m == null || m.getName() == null || m.getName().isBlank()) {
            JsonResponse.badRequest(ctx.getExchange(), "Medicine name and details are required.");
            return;
        }

        boolean ok = medicineDAO.create(m);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "CREATE_MEDICINE", "MEDICINE", String.valueOf(m.getId()), "Created medicine: " + m.getName() + " (" + m.getGenericName() + ")", ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Medicine catalog entry created successfully.", m);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to create medicine. Check for duplicate barcode.");
        }
    }

    public void handleUpdate(RequestContext ctx, int id) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN", "INVENTORY_MANAGER")) {
            JsonResponse.forbidden(ctx.getExchange(), "Only Admin or Inventory Managers can update medicine metadata.");
            return;
        }

        Medicine m = ctx.getBodyAs(Medicine.class);
        if (m == null) {
            JsonResponse.badRequest(ctx.getExchange(), "Invalid medicine data.");
            return;
        }
        m.setId(id);

        boolean ok = medicineDAO.update(m);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "UPDATE_MEDICINE", "MEDICINE", String.valueOf(id), "Updated medicine details for: " + m.getName(), ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Medicine updated successfully.", m);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to update medicine.");
        }
    }

    public void handleDelete(RequestContext ctx, int id) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN")) {
            JsonResponse.forbidden(ctx.getExchange(), "Only administrators can remove medicines.");
            return;
        }

        boolean ok = medicineDAO.delete(id);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "DELETE_MEDICINE", "MEDICINE", String.valueOf(id), "Deactivated medicine ID: " + id, ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Medicine deactivated successfully.", null);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to delete medicine.");
        }
    }
}
