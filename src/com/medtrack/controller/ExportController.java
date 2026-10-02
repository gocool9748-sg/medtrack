package com.medtrack.controller;

import com.google.gson.JsonObject;
import com.medtrack.config.AppConfig;
import com.medtrack.dao.BatchDAO;
import com.medtrack.dao.MedicineDAO;
import com.medtrack.dao.SaleDAO;
import com.medtrack.db.DatabaseSeeder;
import com.medtrack.model.Batch;
import com.medtrack.model.Medicine;
import com.medtrack.model.Sale;
import com.medtrack.model.User;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;
import com.medtrack.service.AuthService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExportController {
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final BatchDAO batchDAO = new BatchDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final AuthService authService;

    public ExportController(AuthService authService) {
        this.authService = authService;
    }

    public void handleExportCsv(RequestContext ctx, String type) throws IOException {
        StringBuilder csv = new StringBuilder();
        String filename = "medtrack-export.csv";

        if ("medicines".equalsIgnoreCase(type)) {
            filename = "medicines-catalog.csv";
            csv.append("ID,Name,Generic Name,Category,Dosage Form,Strength,Unit,Reorder Level,Requires Rx,Total Stock,Min Price,Max Price\n");
            List<Medicine> list = medicineDAO.findAll(null, null, null);
            for (Medicine m : list) {
                csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%s,%d,%.2f,%.2f\n",
                    m.getId(), escapeCsv(m.getName()), escapeCsv(m.getGenericName()), escapeCsv(m.getCategoryName()),
                    escapeCsv(m.getDosageForm()), escapeCsv(m.getStrength()), escapeCsv(m.getUnit()),
                    m.getReorderLevel(), m.isRequiresPrescription() ? "YES" : "NO", m.getTotalStock(), m.getMinPrice(), m.getMaxPrice()));
            }
        } else if ("batches".equalsIgnoreCase(type) || "expiry".equalsIgnoreCase(type)) {
            filename = "batch-expiry-report.csv";
            csv.append("ID,Batch No,Medicine Name,Generic Name,Mfg Date,Expiry Date,Days Remaining,Status,Quantity,Unit Cost,Unit Price,Discount %,Shelf Location,Supplier\n");
            List<Batch> list = batchDAO.findAll(null, null, null, null);
            for (Batch b : list) {
                csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",%s,%s,%d,%s,%d,%.2f,%.2f,%.1f,\"%s\",\"%s\"\n",
                    b.getId(), escapeCsv(b.getBatchNumber()), escapeCsv(b.getMedicineName()), escapeCsv(b.getGenericName()),
                    b.getMfgDate(), b.getExpiryDate(), b.getDaysToExpiry(), b.getExpiryStatus(), b.getQuantity(),
                    b.getUnitCost(), b.getUnitPrice(), b.getDiscountPercent(), escapeCsv(b.getShelfLocation()), escapeCsv(b.getSupplierName())));
            }
        } else if ("sales".equalsIgnoreCase(type)) {
            filename = "sales-ledger.csv";
            csv.append("Invoice No,Date,Customer Name,Phone,Doctor,Prescription No,Total Amount,Discount,Tax,Final Amount,Payment Method,Cashier\n");
            List<Sale> list = saleDAO.findAll(null, null, null, 1000);
            for (Sale s : list) {
                csv.append(String.format("\"%s\",%s,\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%.2f,%.2f,%.2f,%s,\"%s\"\n",
                    escapeCsv(s.getInvoiceNumber()), s.getSaleDate(), escapeCsv(s.getCustomerName()),
                    escapeCsv(s.getCustomerPhone()), escapeCsv(s.getDoctorName()), escapeCsv(s.getPrescriptionNo()),
                    s.getTotalAmount(), s.getDiscountAmount(), s.getTaxAmount(), s.getFinalAmount(), s.getPaymentMethod(), escapeCsv(s.getCashierName())));
            }
        }

        HttpExchange exchange = ctx.getExchange();
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void handleResetData(RequestContext ctx) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(user, "ADMIN")) {
            JsonResponse.forbidden(ctx.getExchange(), "Only administrators can reset and re-seed data.");
            return;
        }

        DatabaseSeeder.seedIfNeeded();
        JsonResponse.ok(ctx.getExchange(), "Data seed verification complete.", null);
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }
}
