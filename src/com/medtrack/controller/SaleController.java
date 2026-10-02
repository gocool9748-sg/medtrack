package com.medtrack.controller;

import com.medtrack.dao.AuditLogDAO;
import com.medtrack.dao.SaleDAO;
import com.medtrack.model.Sale;
import com.medtrack.model.User;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;
import com.medtrack.service.AuthService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class SaleController {
    private final SaleDAO saleDAO = new SaleDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final AuthService authService;

    public SaleController(AuthService authService) {
        this.authService = authService;
    }

    public void handleGetAll(RequestContext ctx) throws IOException {
        String search = ctx.getQueryParam("search");
        String dateFrom = ctx.getQueryParam("from");
        String dateTo = ctx.getQueryParam("to");
        Integer limit = ctx.getQueryParamAsInt("limit");

        List<Sale> sales = saleDAO.findAll(search, dateFrom, dateTo, limit);
        JsonResponse.ok(ctx.getExchange(), sales);
    }

    public void handleGetById(RequestContext ctx, int id) throws IOException {
        Sale sale = saleDAO.findById(id);
        if (sale == null) {
            JsonResponse.notFound(ctx.getExchange(), "Invoice not found.");
            return;
        }
        JsonResponse.ok(ctx.getExchange(), sale);
    }

    public void handleGetByInvoiceNumber(RequestContext ctx, String invoiceNo) throws IOException {
        Sale sale = saleDAO.findByInvoiceNumber(invoiceNo);
        if (sale == null) {
            JsonResponse.notFound(ctx.getExchange(), "Invoice not found for number: " + invoiceNo);
            return;
        }
        JsonResponse.ok(ctx.getExchange(), sale);
    }

    public void handleCreate(RequestContext ctx) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }

        Sale sale = ctx.getBodyAs(Sale.class);
        if (sale == null || sale.getItems() == null || sale.getItems().isEmpty()) {
            JsonResponse.badRequest(ctx.getExchange(), "Cannot complete sale without line items.");
            return;
        }

        sale.setCashierId(user.getId());

        try {
            Sale completedSale = saleDAO.createSale(sale);
            auditLogDAO.log(user.getId(), user.getUsername(), user.getRole(), "DISPENSE_SALE", "SALE", completedSale.getInvoiceNumber(), "Completed POS checkout for " + completedSale.getCustomerName() + " (Total: $" + String.format("%.2f", completedSale.getFinalAmount()) + ", " + completedSale.getItems().size() + " items)", ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "Sale completed successfully. Invoice generated.", completedSale);
        } catch (SQLException e) {
            JsonResponse.error(ctx.getExchange(), "Failed to process sale: " + e.getMessage());
        }
    }
}
