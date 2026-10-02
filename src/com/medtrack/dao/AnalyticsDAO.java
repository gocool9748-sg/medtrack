package com.medtrack.dao;

import com.medtrack.db.DatabaseManager;

import java.sql.*;
import java.util.*;

public class AnalyticsDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            
            // 1. Medicines & Stock totals
            try (ResultSet rs = stmt.executeQuery("""
                SELECT 
                    COUNT(DISTINCT m.id) as total_medicines,
                    COALESCE(SUM(CASE WHEN b.quantity > 0 AND b.status != 'EXPIRED' AND b.status != 'QUARANTINED' THEN b.quantity ELSE 0 END), 0) as total_stock_units,
                    COALESCE(SUM(CASE WHEN b.quantity > 0 AND b.status != 'EXPIRED' AND b.status != 'QUARANTINED' THEN b.quantity * b.unit_cost ELSE 0 END), 0.0) as inventory_cost_value,
                    COALESCE(SUM(CASE WHEN b.quantity > 0 AND b.status != 'EXPIRED' AND b.status != 'QUARANTINED' THEN b.quantity * b.unit_price ELSE 0 END), 0.0) as inventory_retail_value
                FROM medicines m
                LEFT JOIN batches b ON m.id = b.medicine_id
                WHERE m.is_active = 1;
            """)) {
                if (rs.next()) {
                    stats.put("totalMedicines", rs.getInt("total_medicines"));
                    stats.put("totalStockUnits", rs.getInt("total_stock_units"));
                    stats.put("inventoryCostValue", rs.getDouble("inventory_cost_value"));
                    stats.put("inventoryRetailValue", rs.getDouble("inventory_retail_value"));
                }
            }

            // 2. Batch Expiry Breakdown
            try (ResultSet rs = stmt.executeQuery("""
                SELECT
                    COUNT(id) as total_batches,
                    COUNT(CASE WHEN expiry_date < date('now') OR status = 'EXPIRED' THEN 1 END) as expired_count,
                    COALESCE(SUM(CASE WHEN expiry_date < date('now') OR status = 'EXPIRED' THEN quantity * unit_cost ELSE 0 END), 0.0) as expired_loss_value,
                    COUNT(CASE WHEN expiry_date >= date('now') AND expiry_date <= date('now', '+30 day') AND status != 'QUARANTINED' THEN 1 END) as critical_count,
                    COALESCE(SUM(CASE WHEN expiry_date >= date('now') AND expiry_date <= date('now', '+30 day') AND status != 'QUARANTINED' THEN quantity * unit_cost ELSE 0 END), 0.0) as critical_value,
                    COUNT(CASE WHEN expiry_date > date('now', '+30 day') AND expiry_date <= date('now', '+90 day') AND status != 'QUARANTINED' THEN 1 END) as warning_count,
                    COALESCE(SUM(CASE WHEN expiry_date > date('now', '+30 day') AND expiry_date <= date('now', '+90 day') AND status != 'QUARANTINED' THEN quantity * unit_cost ELSE 0 END), 0.0) as warning_value,
                    COUNT(CASE WHEN expiry_date > date('now', '+90 day') AND status = 'ACTIVE' THEN 1 END) as safe_count,
                    COUNT(CASE WHEN status = 'QUARANTINED' THEN 1 END) as quarantined_count
                FROM batches
                WHERE quantity > 0 OR status = 'EXPIRED';
            """)) {
                if (rs.next()) {
                    stats.put("totalBatches", rs.getInt("total_batches"));
                    stats.put("expiredCount", rs.getInt("expired_count"));
                    stats.put("expiredLossValue", rs.getDouble("expired_loss_value"));
                    stats.put("criticalCount", rs.getInt("critical_count"));
                    stats.put("criticalValue", rs.getDouble("critical_value"));
                    stats.put("warningCount", rs.getInt("warning_count"));
                    stats.put("warningValue", rs.getDouble("warning_value"));
                    stats.put("safeCount", rs.getInt("safe_count"));
                    stats.put("quarantinedCount", rs.getInt("quarantined_count"));
                }
            }

            // 3. Low stock count
            try (ResultSet rs = stmt.executeQuery("""
                SELECT COUNT(*) as low_stock_count FROM (
                    SELECT m.id, m.reorder_level, COALESCE(SUM(CASE WHEN b.quantity > 0 AND b.status != 'EXPIRED' AND b.status != 'QUARANTINED' THEN b.quantity ELSE 0 END), 0) as current_stock
                    FROM medicines m
                    LEFT JOIN batches b ON m.id = b.medicine_id
                    WHERE m.is_active = 1
                    GROUP BY m.id
                    HAVING current_stock <= m.reorder_level
                );
            """)) {
                if (rs.next()) {
                    stats.put("lowStockCount", rs.getInt("low_stock_count"));
                }
            }

            // 4. Sales metrics (Today & Month)
            try (ResultSet rs = stmt.executeQuery("""
                SELECT
                    COALESCE(SUM(CASE WHEN date(sale_date) = date('now') THEN final_amount ELSE 0 END), 0.0) as today_sales_amount,
                    COUNT(CASE WHEN date(sale_date) = date('now') THEN 1 END) as today_sales_count,
                    COALESCE(SUM(CASE WHEN strftime('%Y-%m', sale_date) = strftime('%Y-%m', 'now') THEN final_amount ELSE 0 END), 0.0) as month_sales_amount,
                    COUNT(CASE WHEN strftime('%Y-%m', sale_date) = strftime('%Y-%m', 'now') THEN 1 END) as month_sales_count
                FROM sales;
            """)) {
                if (rs.next()) {
                    stats.put("todaySalesAmount", rs.getDouble("today_sales_amount"));
                    stats.put("todaySalesCount", rs.getInt("today_sales_count"));
                    stats.put("monthSalesAmount", rs.getDouble("month_sales_amount"));
                    stats.put("monthSalesCount", rs.getInt("month_sales_count"));
                }
            }

            // 5. Total Disposals loss
            try (ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(total_loss), 0.0) as total_disposal_loss, COUNT(*) as total_disposals FROM disposals;")) {
                if (rs.next()) {
                    stats.put("totalDisposalLoss", rs.getDouble("total_disposal_loss"));
                    stats.put("totalDisposalsCount", rs.getInt("total_disposals"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    public List<Map<String, Object>> getStockByCategory() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT c.name as category_name, c.icon,
                   COUNT(DISTINCT m.id) as medicine_count,
                   COALESCE(SUM(b.quantity), 0) as total_units,
                   COALESCE(SUM(b.quantity * b.unit_price), 0.0) as retail_value,
                   COALESCE(SUM(b.quantity * b.unit_cost), 0.0) as cost_value
            FROM categories c
            LEFT JOIN medicines m ON c.id = m.category_id AND m.is_active = 1
            LEFT JOIN batches b ON m.id = b.medicine_id AND b.quantity > 0 AND b.status != 'EXPIRED' AND b.status != 'QUARANTINED'
            GROUP BY c.id
            ORDER BY retail_value DESC;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("categoryName", rs.getString("category_name"));
                item.put("icon", rs.getString("icon"));
                item.put("medicineCount", rs.getInt("medicine_count"));
                item.put("totalUnits", rs.getInt("total_units"));
                item.put("retailValue", rs.getDouble("retail_value"));
                item.put("costValue", rs.getDouble("cost_value"));
                list.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> getTopSellingMedicines(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT m.id, m.name, m.generic_name, m.dosage_form, c.name as category_name,
                   COALESCE(SUM(si.quantity), 0) as total_sold,
                   COALESCE(SUM(si.subtotal), 0.0) as total_revenue
            FROM sale_items si
            JOIN medicines m ON si.medicine_id = m.id
            LEFT JOIN categories c ON m.category_id = c.id
            GROUP BY m.id
            ORDER BY total_sold DESC
            LIMIT ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 5);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("name", rs.getString("name"));
                    item.put("genericName", rs.getString("generic_name"));
                    item.put("dosageForm", rs.getString("dosage_form"));
                    item.put("categoryName", rs.getString("category_name"));
                    item.put("totalSold", rs.getInt("total_sold"));
                    item.put("totalRevenue", rs.getDouble("total_revenue"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> getMonthlyFinancials() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT strftime('%Y-%m', sale_date) as month,
                   COUNT(id) as transactions,
                   COALESCE(SUM(final_amount), 0.0) as revenue,
                   COALESCE(SUM(discount_amount), 0.0) as discounts,
                   COALESCE(SUM(tax_amount), 0.0) as tax
            FROM sales
            GROUP BY strftime('%Y-%m', sale_date)
            ORDER BY month ASC
            LIMIT 12;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("month", rs.getString("month"));
                item.put("transactions", rs.getInt("transactions"));
                item.put("revenue", rs.getDouble("revenue"));
                item.put("discounts", rs.getDouble("discounts"));
                item.put("tax", rs.getDouble("tax"));
                list.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
