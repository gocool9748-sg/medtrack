package com.medtrack.dao;

import com.medtrack.db.DatabaseManager;
import com.medtrack.model.PurchaseOrder;
import com.medtrack.model.PurchaseOrderItem;
import com.medtrack.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseOrderDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public PurchaseOrder create(PurchaseOrder po) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            if (po.getPoNumber() == null || po.getPoNumber().isBlank()) {
                po.setPoNumber("PO-" + System.currentTimeMillis() % 10000000);
            }
            if (po.getOrderDate() == null || po.getOrderDate().isBlank()) {
                po.setOrderDate(DateUtils.today());
            }

            String sql = """
                INSERT INTO purchase_orders (po_number, supplier_id, order_date, expected_date, 
                                             total_amount, status, created_by, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """;

            int poId = 0;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, po.getPoNumber());
                ps.setInt(2, po.getSupplierId());
                ps.setString(3, po.getOrderDate());
                ps.setString(4, po.getExpectedDate());
                ps.setDouble(5, po.getTotalAmount());
                ps.setString(6, po.getStatus() != null ? po.getStatus() : "SENT");
                if (po.getCreatedBy() > 0) {
                    ps.setInt(7, po.getCreatedBy());
                } else {
                    ps.setNull(7, Types.INTEGER);
                }
                ps.setString(8, po.getNotes());

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        poId = rs.getInt(1);
                        po.setId(poId);
                    }
                }
            }

            String itemSql = "INSERT INTO purchase_order_items (po_id, medicine_id, quantity, unit_cost, subtotal) VALUES (?, ?, ?, ?, ?);";
            try (PreparedStatement psItem = conn.prepareStatement(itemSql, Statement.RETURN_GENERATED_KEYS)) {
                for (PurchaseOrderItem item : po.getItems()) {
                    psItem.setInt(1, poId);
                    psItem.setInt(2, item.getMedicineId());
                    psItem.setInt(3, item.getQuantity());
                    psItem.setDouble(4, item.getUnitCost());
                    psItem.setDouble(5, item.getSubtotal());
                    psItem.addBatch();
                }
                psItem.executeBatch();
            }

            conn.commit();
            return po;

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

    public List<PurchaseOrder> findAll(String status) {
        List<PurchaseOrder> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT po.*, s.name as supplier_name, u.full_name as creator_name, COUNT(poi.id) as item_count
            FROM purchase_orders po
            JOIN suppliers s ON po.supplier_id = s.id
            LEFT JOIN users u ON po.created_by = u.id
            LEFT JOIN purchase_order_items poi ON po.id = poi.po_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND po.status = ?");
            params.add(status.toUpperCase());
        }

        sql.append(" GROUP BY po.id ORDER BY po.order_date DESC, po.id DESC;");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPurchaseOrder(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public PurchaseOrder findById(int id) {
        String sql = """
            SELECT po.*, s.name as supplier_name, u.full_name as creator_name
            FROM purchase_orders po
            JOIN suppliers s ON po.supplier_id = s.id
            LEFT JOIN users u ON po.created_by = u.id
            WHERE po.id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PurchaseOrder po = mapPurchaseOrder(rs);
                    po.setItems(findItemsByPoId(id, conn));
                    return po;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateStatus(int poId, String status) {
        String sql = "UPDATE purchase_orders SET status = ?, received_date = CASE WHEN ? = 'RECEIVED' THEN ? ELSE received_date END WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.toUpperCase());
            ps.setString(2, status.toUpperCase());
            ps.setString(3, DateUtils.today());
            ps.setInt(4, poId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private List<PurchaseOrderItem> findItemsByPoId(int poId, Connection conn) throws SQLException {
        List<PurchaseOrderItem> items = new ArrayList<>();
        String sql = """
            SELECT poi.*, m.name as medicine_name, m.generic_name
            FROM purchase_order_items poi
            JOIN medicines m ON poi.medicine_id = m.id
            WHERE poi.po_id = ?;
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, poId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PurchaseOrderItem item = new PurchaseOrderItem();
                    item.setId(rs.getInt("id"));
                    item.setPoId(rs.getInt("po_id"));
                    item.setMedicineId(rs.getInt("medicine_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setGenericName(rs.getString("generic_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitCost(rs.getDouble("unit_cost"));
                    item.setSubtotal(rs.getDouble("subtotal"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private PurchaseOrder mapPurchaseOrder(ResultSet rs) throws SQLException {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(rs.getInt("id"));
        po.setPoNumber(rs.getString("po_number"));
        po.setSupplierId(rs.getInt("supplier_id"));
        po.setSupplierName(rs.getString("supplier_name"));
        po.setOrderDate(rs.getString("order_date"));
        po.setExpectedDate(rs.getString("expected_date"));
        po.setReceivedDate(rs.getString("received_date"));
        po.setTotalAmount(rs.getDouble("total_amount"));
        po.setStatus(rs.getString("status"));
        po.setCreatedBy(rs.getInt("created_by"));
        po.setCreatorName(rs.getString("creator_name"));
        po.setNotes(rs.getString("notes"));
        return po;
    }
}
