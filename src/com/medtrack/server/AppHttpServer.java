package com.medtrack.server;

import com.medtrack.config.AppConfig;
import com.medtrack.controller.*;
import com.medtrack.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class AppHttpServer {
    private final HttpServer server;
    private final AuthService authService = new AuthService();

    // Controllers
    private final AuthController authController = new AuthController(authService);
    private final MedicineController medicineController = new MedicineController(authService);
    private final BatchController batchController = new BatchController(authService);
    private final CategoryController categoryController = new CategoryController(authService);
    private final SupplierController supplierController = new SupplierController(authService);
    private final SaleController saleController = new SaleController(authService);
    private final PurchaseOrderController poController = new PurchaseOrderController(authService);
    private final DisposalController disposalController = new DisposalController(authService);
    private final ExpiryController expiryController = new ExpiryController(authService);
    private final AnalyticsController analyticsController = new AnalyticsController();
    private final AuditController auditController = new AuditController();
    private final NotificationController notificationController = new NotificationController();
    private final ExportController exportController = new ExportController(authService);

    public AppHttpServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        this.server.setExecutor(Executors.newCachedThreadPool());

        // Map API Context
        this.server.createContext("/api/", new ApiDispatcher());

        // Map Static File Context
        this.server.createContext("/", new StaticFileHandler("web"));
    }

    public void start() {
        server.start();
        System.out.println("MedTrack HTTP Server running on http://localhost:" + AppConfig.PORT);
    }

    public void stop() {
        server.stop(0);
        System.out.println("MedTrack HTTP Server stopped.");
    }

    private class ApiDispatcher implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS Pre-flight
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            RequestContext ctx = new RequestContext(exchange);
            String path = ctx.getPath();
            String method = ctx.getMethod();

            try {
                // ==================== AUTH ROUTES ====================
                if (path.equals("/api/auth/login") && method.equals("POST")) {
                    authController.handleLogin(ctx);
                } else if (path.equals("/api/auth/me") && method.equals("GET")) {
                    authController.handleMe(ctx);
                } else if (path.equals("/api/auth/logout") && method.equals("POST")) {
                    authController.handleLogout(ctx);
                } else if (path.equals("/api/auth/users") && method.equals("GET")) {
                    authController.handleGetUsers(ctx);
                } else if (path.equals("/api/auth/users") && method.equals("POST")) {
                    authController.handleCreateUser(ctx);

                // ==================== MEDICINES ROUTES ====================
                } else if (path.equals("/api/medicines") && method.equals("GET")) {
                    medicineController.handleGetAll(ctx);
                } else if (path.equals("/api/medicines") && method.equals("POST")) {
                    medicineController.handleCreate(ctx);
                } else if (path.startsWith("/api/medicines/barcode/")) {
                    String barcode = path.substring("/api/medicines/barcode/".length());
                    medicineController.handleGetByBarcode(ctx, barcode);
                } else if (path.matches("^/api/medicines/\\d+$")) {
                    int id = Integer.parseInt(path.substring("/api/medicines/".length()));
                    if (method.equals("GET")) medicineController.handleGetById(ctx, id);
                    else if (method.equals("PUT")) medicineController.handleUpdate(ctx, id);
                    else if (method.equals("DELETE")) medicineController.handleDelete(ctx, id);

                // ==================== BATCHES ROUTES ====================
                } else if (path.equals("/api/batches") && method.equals("GET")) {
                    batchController.handleGetAll(ctx);
                } else if (path.equals("/api/batches") && method.equals("POST")) {
                    batchController.handleCreate(ctx);
                } else if (path.startsWith("/api/batches/fefo/")) {
                    int medId = Integer.parseInt(path.substring("/api/batches/fefo/".length()));
                    batchController.handleGetFEFO(ctx, medId);
                } else if (path.matches("^/api/batches/\\d+/quarantine$") && method.equals("POST")) {
                    int id = extractIdFromPath(path, "/api/batches/", "/quarantine");
                    batchController.handleQuarantine(ctx, id);
                } else if (path.matches("^/api/batches/\\d+/discount$") && method.equals("PUT")) {
                    int id = extractIdFromPath(path, "/api/batches/", "/discount");
                    batchController.handleDiscount(ctx, id);
                } else if (path.matches("^/api/batches/\\d+/adjust$") && method.equals("PUT")) {
                    int id = extractIdFromPath(path, "/api/batches/", "/adjust");
                    batchController.handleStockAdjustment(ctx, id);
                } else if (path.matches("^/api/batches/\\d+$")) {
                    int id = Integer.parseInt(path.substring("/api/batches/".length()));
                    if (method.equals("GET")) batchController.handleGetById(ctx, id);
                    else if (method.equals("PUT")) batchController.handleUpdate(ctx, id);
                    else if (method.equals("DELETE")) batchController.handleDelete(ctx, id);

                // ==================== CATEGORIES ROUTES ====================
                } else if (path.equals("/api/categories") && method.equals("GET")) {
                    categoryController.handleGetAll(ctx);
                } else if (path.equals("/api/categories") && method.equals("POST")) {
                    categoryController.handleCreate(ctx);
                } else if (path.matches("^/api/categories/\\d+$")) {
                    int id = Integer.parseInt(path.substring("/api/categories/".length()));
                    if (method.equals("GET")) categoryController.handleGetById(ctx, id);
                    else if (method.equals("PUT")) categoryController.handleUpdate(ctx, id);
                    else if (method.equals("DELETE")) categoryController.handleDelete(ctx, id);

                // ==================== SUPPLIERS ROUTES ====================
                } else if (path.equals("/api/suppliers") && method.equals("GET")) {
                    supplierController.handleGetAll(ctx);
                } else if (path.equals("/api/suppliers") && method.equals("POST")) {
                    supplierController.handleCreate(ctx);
                } else if (path.matches("^/api/suppliers/\\d+$")) {
                    int id = Integer.parseInt(path.substring("/api/suppliers/".length()));
                    if (method.equals("GET")) supplierController.handleGetById(ctx, id);
                    else if (method.equals("PUT")) supplierController.handleUpdate(ctx, id);
                    else if (method.equals("DELETE")) supplierController.handleDelete(ctx, id);

                // ==================== SALES & POS ROUTES ====================
                } else if (path.equals("/api/sales") && method.equals("GET")) {
                    saleController.handleGetAll(ctx);
                } else if (path.equals("/api/sales") && method.equals("POST")) {
                    saleController.handleCreate(ctx);
                } else if (path.startsWith("/api/sales/invoice/")) {
                    String invoiceNo = path.substring("/api/sales/invoice/".length());
                    saleController.handleGetByInvoiceNumber(ctx, invoiceNo);
                } else if (path.matches("^/api/sales/\\d+$") && method.equals("GET")) {
                    int id = Integer.parseInt(path.substring("/api/sales/".length()));
                    saleController.handleGetById(ctx, id);

                // ==================== PURCHASE ORDERS ROUTES ====================
                } else if (path.equals("/api/orders") && method.equals("GET")) {
                    poController.handleGetAll(ctx);
                } else if (path.equals("/api/orders") && method.equals("POST")) {
                    poController.handleCreate(ctx);
                } else if (path.matches("^/api/orders/\\d+/status$") && method.equals("PUT")) {
                    int id = extractIdFromPath(path, "/api/orders/", "/status");
                    poController.handleUpdateStatus(ctx, id);
                } else if (path.matches("^/api/orders/\\d+$") && method.equals("GET")) {
                    int id = Integer.parseInt(path.substring("/api/orders/".length()));
                    poController.handleGetById(ctx, id);

                // ==================== DISPOSALS ROUTES ====================
                } else if (path.equals("/api/disposals") && method.equals("GET")) {
                    disposalController.handleGetAll(ctx);
                } else if (path.equals("/api/disposals") && method.equals("POST")) {
                    disposalController.handleCreate(ctx);

                // ==================== EXPIRY INTELLIGENCE ROUTES ====================
                } else if (path.equals("/api/expiry/recommendations") && method.equals("GET")) {
                    expiryController.handleRecommendations(ctx);
                } else if (path.equals("/api/expiry/scan") && method.equals("POST")) {
                    expiryController.handleScan(ctx);
                } else if (path.equals("/api/expiry/markdown") && method.equals("POST")) {
                    expiryController.handleApplyMarkdown(ctx);

                // ==================== ANALYTICS ROUTES ====================
                } else if (path.equals("/api/analytics/dashboard") && method.equals("GET")) {
                    analyticsController.handleDashboardStats(ctx);
                } else if (path.equals("/api/analytics/categories") && method.equals("GET")) {
                    analyticsController.handleCategoryStats(ctx);
                } else if (path.equals("/api/analytics/top-medicines") && method.equals("GET")) {
                    analyticsController.handleTopMedicines(ctx);
                } else if (path.equals("/api/analytics/financials") && method.equals("GET")) {
                    analyticsController.handleMonthlyFinancials(ctx);

                // ==================== AUDIT ROUTES ====================
                } else if (path.equals("/api/audit") && method.equals("GET")) {
                    auditController.handleGetAll(ctx);

                // ==================== NOTIFICATIONS ROUTES ====================
                } else if (path.equals("/api/notifications") && method.equals("GET")) {
                    notificationController.handleGetAll(ctx);
                } else if (path.matches("^/api/notifications/\\d+/read$") && method.equals("PUT")) {
                    int id = extractIdFromPath(path, "/api/notifications/", "/read");
                    notificationController.handleMarkRead(ctx, id);
                } else if (path.equals("/api/notifications/read-all") && method.equals("PUT")) {
                    notificationController.handleMarkAllRead(ctx);

                // ==================== EXPORT & BACKUP ROUTES ====================
                } else if (path.startsWith("/api/export/csv/")) {
                    String type = path.substring("/api/export/csv/".length());
                    exportController.handleExportCsv(ctx, type);
                } else if (path.equals("/api/backup/reset") && method.equals("POST")) {
                    exportController.handleResetData(ctx);

                } else {
                    JsonResponse.notFound(ctx.getExchange(), "Endpoint not found: " + method + " " + path);
                }
            } catch (Exception e) {
                e.printStackTrace();
                JsonResponse.error(ctx.getExchange(), "Internal error processing request: " + e.getMessage());
            }
        }

        private int extractIdFromPath(String path, String prefix, String suffix) {
            String mid = path.substring(prefix.length());
            if (suffix != null && mid.endsWith(suffix)) {
                mid = mid.substring(0, mid.length() - suffix.length());
            }
            return Integer.parseInt(mid);
        }
    }
}
