package com.medtrack.controller;

import com.medtrack.dao.AnalyticsDAO;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AnalyticsController {
    private final AnalyticsDAO analyticsDAO = new AnalyticsDAO();

    public void handleDashboardStats(RequestContext ctx) throws IOException {
        Map<String, Object> stats = analyticsDAO.getDashboardStats();
        JsonResponse.ok(ctx.getExchange(), stats);
    }

    public void handleCategoryStats(RequestContext ctx) throws IOException {
        List<Map<String, Object>> list = analyticsDAO.getStockByCategory();
        JsonResponse.ok(ctx.getExchange(), list);
    }

    public void handleTopMedicines(RequestContext ctx) throws IOException {
        Integer limit = ctx.getQueryParamAsInt("limit");
        List<Map<String, Object>> list = analyticsDAO.getTopSellingMedicines(limit != null ? limit : 10);
        JsonResponse.ok(ctx.getExchange(), list);
    }

    public void handleMonthlyFinancials(RequestContext ctx) throws IOException {
        List<Map<String, Object>> list = analyticsDAO.getMonthlyFinancials();
        JsonResponse.ok(ctx.getExchange(), list);
    }
}
