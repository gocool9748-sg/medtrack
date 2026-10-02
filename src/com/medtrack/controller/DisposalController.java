package com.medtrack.controller;

import com.medtrack.dao.AuditLogDAO;
import com.medtrack.dao.DisposalDAO;
import com.medtrack.model.DisposalRecord;
import com.medtrack.model.User;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;
import com.medtrack.service.AuthService;

import java.io.IOException;
import java.util.List;

public class DisposalController {
    private final DisposalDAO disposalDAO = new DisposalDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final AuthService authService;

    public DisposalController(AuthService authService) {
        this.authService = authService;
    }

    public void handleGetAll(RequestContext ctx) throws IOException {
        String search = ctx.getQueryParam("search");
        String reason = ctx.getQueryParam("reason");
        List<DisposalRecord> list = disposalDAO.findAll(search, reason);
        JsonResponse.ok(ctx.getExchange(), list);
    }

    public void handleCreate(RequestContext ctx) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN", "INVENTORY_MANAGER", "PHARMACIST")) {
            JsonResponse.forbidden(ctx.getExchange(), "Permission denied to authorize drug disposal.");
            return;
        }

        DisposalRecord d = ctx.getBodyAs(DisposalRecord.class);
        if (d == null || d.getMedicineId() <= 0 || d.getQuantity() <= 0) {
            JsonResponse.badRequest(ctx.getExchange(), "Valid medicine ID and quantity required.");
            return;
        }

        d.setAuthorizedBy(user.getId());
        d.setTotalLoss(d.getQuantity() * d.getUnitCost());

        boolean ok = disposalDAO.create(d);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "DISPOSAL_AUTHORIZED", "DISPOSAL", d.getCertificateNumber(), "Authorized safe destruction of " + d.getQuantity() + " units of " + d.getBatchNumber() + " (Total Loss: $" + String.format("%.2f", d.getTotalLoss()) + ", Method: " + d.getDestructionMethod() + ")", ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Disposal recorded and certificate generated.", d);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to record disposal.");
        }
    }
}
