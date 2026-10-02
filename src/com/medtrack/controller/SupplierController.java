package com.medtrack.controller;

import com.medtrack.dao.AuditLogDAO;
import com.medtrack.dao.SupplierDAO;
import com.medtrack.model.Supplier;
import com.medtrack.model.User;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;
import com.medtrack.service.AuthService;

import java.io.IOException;
import java.util.List;

public class SupplierController {
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final AuthService authService;

    public SupplierController(AuthService authService) {
        this.authService = authService;
    }

    public void handleGetAll(RequestContext ctx) throws IOException {
        List<Supplier> list = supplierDAO.findAll();
        JsonResponse.ok(ctx.getExchange(), list);
    }

    public void handleGetById(RequestContext ctx, int id) throws IOException {
        Supplier s = supplierDAO.findById(id);
        if (s == null) {
            JsonResponse.notFound(ctx.getExchange(), "Supplier not found.");
            return;
        }
        JsonResponse.ok(ctx.getExchange(), s);
    }

    public void handleCreate(RequestContext ctx) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN", "INVENTORY_MANAGER")) {
            JsonResponse.forbidden(ctx.getExchange(), "Permission denied.");
            return;
        }

        Supplier s = ctx.getBodyAs(Supplier.class);
        if (s == null || s.getName() == null || s.getName().isBlank()) {
            JsonResponse.badRequest(ctx.getExchange(), "Supplier company name is required.");
            return;
        }

        boolean ok = supplierDAO.create(s);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "CREATE_SUPPLIER", "SUPPLIER", String.valueOf(s.getId()), "Registered new supplier: " + s.getName(), ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Supplier registered successfully.", s);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to create supplier. Ensure name is unique.");
        }
    }

    public void handleUpdate(RequestContext ctx, int id) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN", "INVENTORY_MANAGER")) {
            JsonResponse.forbidden(ctx.getExchange(), "Permission denied.");
            return;
        }

        Supplier s = ctx.getBodyAs(Supplier.class);
        if (s == null) {
            JsonResponse.badRequest(ctx.getExchange(), "Invalid supplier payload.");
            return;
        }
        s.setId(id);

        boolean ok = supplierDAO.update(s);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "UPDATE_SUPPLIER", "SUPPLIER", String.valueOf(id), "Updated supplier details: " + s.getName(), ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Supplier details updated.", s);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to update supplier.");
        }
    }

    public void handleDelete(RequestContext ctx, int id) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN")) {
            JsonResponse.forbidden(ctx.getExchange(), "Only administrators can delete suppliers.");
            return;
        }

        boolean ok = supplierDAO.delete(id);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "DELETE_SUPPLIER", "SUPPLIER", String.valueOf(id), "Deleted supplier ID: " + id, ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Supplier deleted successfully.", null);
        } else {
            JsonResponse.error(ctx.getExchange(), "Cannot delete supplier with active purchase orders.");
        }
    }
}
