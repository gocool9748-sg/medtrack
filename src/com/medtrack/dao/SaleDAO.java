package com.medtrack.dao;

import com.medtrack.db.DatabaseManager;
import com.medtrack.model.Sale;
import com.medtrack.model.SaleItem;
import com.medtrack.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();
    private final BatchDAO batchDAO = new BatchDAO();

    public Sale createSale(Sale sale) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Generate Invoice Number if absent
            if (sale.getInvoiceNumber() == null || sale.getInvoiceNumber().isBlank()) {
                sale.setInvoiceNumber("INV-" + System.currentTimeMillis() % 10000000);
            }
            if (sale.getSaleDate() == null || sale.getSaleDate().isBlank()) {
                sale.setSaleDate(DateUtils.now());
            }

            // 2. Insert Sale Master Record
            String saleSql = """
                INSERT INTO sales (invoice_number, customer_name, customer_phone, doctor_name, 
                                   prescription_no, total_amount, discount_amount, tax_amount, 
                                   final_amount, payment_method, cashier_id, sale_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;

            int saleId = 0;
            try (PreparedStatement ps = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sale.getInvoiceNumber());
                ps.setString(2, sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in Customer");
                ps.setString(3, sale.getCustomerPhone());
                ps.setString(4, sale.getDoctorName());
                ps.setString(5, sale.getPrescriptionNo());
                ps.setDouble(6, sale.getTotalAmount());
                ps.setDouble(7, sale.getDiscountAmount());
                ps.setDouble(8, sale.getTaxAmount());
                ps.setDouble(9, sale.getFinalAmount());
                ps.setString(10, sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "CASH");
                if (sale.getCashierId() > 0) {
                    ps.setInt(11, sale.getCashierId());
                } else {
                    ps.setNull(11, Types.INTEGER);
                }
                ps.setString(12, sale.getSaleDate());

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        saleId = rs.getInt(1);
                        sale.setId(saleId);
                    }
                }
            }

            // 3. Insert Items and Deduct Stock Batch quantities atomically
            String itemSql = """
                INSERT INTO sale_items (sale_id, medicine_id, batch_id, quantity, unit_price, discount_percent, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?);
            """;

            try (PreparedStatement psItem = conn.prepareStatement(itemSql, Statement.RETURN_GENERATED_KEYS)) {
                for (SaleItem item : sale.getItems()) {
                    // Check stock and deduct
                    boolean deducted = batchDAO.deductQuantity(item.getBatchId(), item.getQuantity(), conn);
                    if (!deducted) {
                        throw new SQLException("Insufficient stock in batch ID " + item.getBatchId() + " or batch is locked.");
                    }

                    psItem.setInt(1, saleId);
                    psItem.setInt(2, item.getMedicineId());
                    psItem.setInt(3, item.getBatchId());
                    psItem.setInt(4, item.getQuantity());
                    psItem.setDouble(5, item.getUnitPrice());
                    psItem.setDouble(6, item.getDiscountPercent());
                    psItem.setDouble(7, item.getSubtotal());
                    psItem.addBatch();
                }
                psItem.executeBatch();
            }

            conn.commit(); // Transaction success!
            return sale;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    public List<Sale> findAll(String search, String dateFrom, String dateTo, Integer limit) {
        List<Sale> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT s.*, u.full_name as cashier_name, COUNT(si.id) as item_count
            FROM sales s
            LEFT JOIN users u ON s.cashier_id = u.id
            LEFT JOIN sale_items si ON s.id = si.sale_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (s.invoice_number LIKE ? OR s.customer_name LIKE ? OR s.customer_phone LIKE ? OR s.prescription_no LIKE ?)");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (dateFrom != null && !dateFrom.isBlank()) {
            sql.append(" AND s.sale_date >= ?");
            params.add(dateFrom + " 00:00:00");
        }

        if (dateTo != null && !dateTo.isBlank()) {
            sql.append(" AND s.sale_date <= ?");
            params.add(dateTo + " 23:59:59");
        }

        sql.append(" GROUP BY s.id ORDER BY s.sale_date DESC");

        if (limit != null && limit > 0) {
            sql.append(" LIMIT ?");
            params.add(limit);
        }

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSaleSummary(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Sale findById(int id) {
        String sql = """
            SELECT s.*, u.full_name as cashier_name
            FROM sales s
            LEFT JOIN users u ON s.cashier_id = u.id
            WHERE s.id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sale s = mapSaleSummary(rs);
                    s.setItems(findItemsBySaleId(id, conn));
                    return s;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Sale findByInvoiceNumber(String invoiceNo) {
        String sql = """
            SELECT s.*, u.full_name as cashier_name
            FROM sales s
            LEFT JOIN users u ON s.cashier_id = u.id
            WHERE s.invoice_number = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sale s = mapSaleSummary(rs);
                    s.setItems(findItemsBySaleId(s.getId(), conn));
                    return s;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<SaleItem> findItemsBySaleId(int saleId, Connection conn) throws SQLException {
        List<SaleItem> items = new ArrayList<>();
        String sql = """
            SELECT si.*, m.name as medicine_name, m.generic_name, b.batch_number, b.expiry_date
            FROM sale_items si
            JOIN medicines m ON si.medicine_id = m.id
            JOIN batches b ON si.batch_id = b.id
            WHERE si.sale_id = ?;
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleItem item = new SaleItem();
                    item.setId(rs.getInt("id"));
                    item.setSaleId(rs.getInt("sale_id"));
                    item.setMedicineId(rs.getInt("medicine_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setGenericName(rs.getString("generic_name"));
                    item.setBatchId(rs.getInt("batch_id"));
                    item.setBatchNumber(rs.getString("batch_number"));
                    item.setExpiryDate(rs.getString("expiry_date"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getDouble("unit_price"));
                    item.setDiscountPercent(rs.getDouble("discount_percent"));
                    item.setSubtotal(rs.getDouble("subtotal"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private Sale mapSaleSummary(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("id"));
        s.setInvoiceNumber(rs.getString("invoice_number"));
        s.setCustomerName(rs.getString("customer_name"));
        s.setCustomerPhone(rs.getString("customer_phone"));
        s.setDoctorName(rs.getString("doctor_name"));
        s.setPrescriptionNo(rs.getString("prescription_no"));
        s.setTotalAmount(rs.getDouble("total_amount"));
        s.setDiscountAmount(rs.getDouble("discount_amount"));
        s.setTaxAmount(rs.getDouble("tax_amount"));
        s.setFinalAmount(rs.getDouble("final_amount"));
        s.setPaymentMethod(rs.getString("payment_method"));
        s.setCashierId(rs.getInt("cashier_id"));
        s.setCashierName(rs.getString("cashier_name"));
        s.setSaleDate(rs.getString("sale_date"));
        return s;
    }
}
