package com.medtrack.controller;

import com.medtrack.dao.AuditLogDAO;
import com.medtrack.model.AuditLog;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;

import java.io.IOException;
import java.util.List;

public class AuditController {
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public void handleGetAll(RequestContext ctx) throws IOException {
        String search = ctx.getQueryParam("search");
        String action = ctx.getQueryParam("action");
        Integer limit = ctx.getQueryParamAsInt("limit");

        List<AuditLog> list = auditLogDAO.findAll(search, action, limit != null ? limit : 100);
        JsonResponse.ok(ctx.getExchange(), list);
    }
}
