package com.medtrack.controller;

import com.google.gson.JsonObject;
import com.medtrack.dao.AuditLogDAO;
import com.medtrack.dao.BatchDAO;
import com.medtrack.model.User;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;
import com.medtrack.service.AuthService;
import com.medtrack.service.ExpiryEngineService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExpiryController {
    private final ExpiryEngineService expiryEngine = new ExpiryEngineService();
    private final BatchDAO batchDAO = new BatchDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final AuthService authService;

    public ExpiryController(AuthService authService) {
        this.authService = authService;
    }

    public void handleRecommendations(RequestContext ctx) throws IOException {
        List<ExpiryEngineService.MarkdownRecommendation> recs = expiryEngine.generateMarkdownRecommendations();
        JsonResponse.ok(ctx.getExchange(), recs);
    }

    public void handleScan(RequestContext ctx) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }

        Map<String, Object> res = expiryEngine.runAutomatedExpiryScan();
        auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "EXPIRY_SCAN", "SYSTEM", "SCAN_ALL", (String) res.get("message"), ctx.getIpAddress());
        JsonResponse.ok(ctx.getExchange(), res);
    }

    public void handleApplyMarkdown(RequestContext ctx) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }

        JsonObject body = ctx.getBodyAsJsonObject();
        int batchId = body.get("batchId").getAsInt();
        double discount = body.get("discountPercent").getAsDouble();

        boolean ok = batchDAO.updateDiscount(batchId, discount);
        if (ok) {
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "APPLY_MARKDOWN", "BATCH", String.valueOf(batchId), "Applied AI suggested markdown discount of " + discount + "% to batch ID: " + batchId, ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Markdown discount applied successfully.", null);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to apply markdown discount.");
        }
    }
}
