package com.medtrack.controller;

import com.medtrack.dao.AuditLogDAO;
import com.medtrack.dao.CategoryDAO;
import com.medtrack.model.Category;
import com.medtrack.model.User;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;
import com.medtrack.service.AuthService;

import java.io.IOException;
import java.util.List;

public class CategoryController {
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final AuthService authService;

    public CategoryController(AuthService authService) {
        this.authService = authService;
    }

    public void handleGetAll(RequestContext ctx) throws IOException {
        List<Category> list = categoryDAO.findAll();
        JsonResponse.ok(ctx.getExchange(), list);
    }

    public void handleGetById(RequestContext ctx, int id) throws IOException {
        Category c = categoryDAO.findById(id);
        if (c == null) {
            JsonResponse.notFound(ctx.getExchange(), "Category not found.");
            return;
        }
        JsonResponse.ok(ctx.getExchange(), c);
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

        Category cat = ctx.getBodyAs(Category.class);
        if (cat == null || cat.getName() == null || cat.getName().isBlank()) {
            JsonResponse.badRequest(ctx.getExchange(), "Category name is required.");
            return;
        }

        boolean ok = categoryDAO.create(cat);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "CREATE_CATEGORY", "CATEGORY", String.valueOf(cat.getId()), "Created category: " + cat.getName(), ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Category created successfully.", cat);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to create category. Name must be unique.");
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

        Category cat = ctx.getBodyAs(Category.class);
        if (cat == null) {
            JsonResponse.badRequest(ctx.getExchange(), "Invalid category payload.");
            return;
        }
        cat.setId(id);

        boolean ok = categoryDAO.update(cat);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "UPDATE_CATEGORY", "CATEGORY", String.valueOf(id), "Updated category: " + cat.getName(), ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Category updated successfully.", cat);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to update category.");
        }
    }

    public void handleDelete(RequestContext ctx, int id) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN")) {
            JsonResponse.forbidden(ctx.getExchange(), "Only administrators can delete categories.");
            return;
        }

        boolean ok = categoryDAO.delete(id);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "DELETE_CATEGORY", "CATEGORY", String.valueOf(id), "Deleted category ID: " + id, ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Category deleted successfully.", null);
        } else {
            JsonResponse.error(ctx.getExchange(), "Cannot delete category with associated active medicines.");
        }
    }
}
