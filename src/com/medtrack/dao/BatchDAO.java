package com.medtrack.dao;

import com.medtrack.db.DatabaseManager;
import com.medtrack.model.Batch;
import com.medtrack.util.DateUtils;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BatchDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public List<Batch> findAll(String search, String statusFilter, Integer medicineId, Integer daysFilter) {
        List<Batch> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT b.*, m.name as medicine_name, m.generic_name, m.dosage_form, m.strength, 
                   c.name as category_name, s.name as supplier_name
            FROM batches b
            JOIN medicines m ON b.medicine_id = m.id
            LEFT JOIN categories c ON m.category_id = c.id
            LEFT JOIN suppliers s ON b.supplier_id = s.id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (medicineId != null && medicineId > 0) {
            sql.append(" AND b.medicine_id = ?");
            params.add(medicineId);
        }

        if (search != null && !search.isBlank()) {
            sql.append(" AND (b.batch_number LIKE ? OR m.name LIKE ? OR m.generic_name LIKE ? OR b.shelf_location LIKE ?)");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            if ("EXPIRED".equalsIgnoreCase(statusFilter)) {
                sql.append(" AND (b.expiry_date < date('now') OR b.status = 'EXPIRED')");
            } else if ("CRITICAL".equalsIgnoreCase(statusFilter)) {
                sql.append(" AND b.expiry_date >= date('now') AND b.expiry_date <= date('now', '+30 day') AND b.status != 'QUARANTINED'");
            } else if ("WARNING".equalsIgnoreCase(statusFilter)) {
                sql.append(" AND b.expiry_date > date('now', '+30 day') AND b.expiry_date <= date('now', '+90 day') AND b.status != 'QUARANTINED'");
            } else if ("SAFE".equalsIgnoreCase(statusFilter)) {
                sql.append(" AND b.expiry_date > date('now', '+90 day') AND b.status = 'ACTIVE'");
            } else if ("QUARANTINED".equalsIgnoreCase(statusFilter)) {
                sql.append(" AND b.status = 'QUARANTINED'");
            }
        }

        if (daysFilter != null && daysFilter > 0) {
            sql.append(" AND b.expiry_date <= date('now', '+' || ? || ' day') AND b.expiry_date >= date('now')");
            params.add(daysFilter);
        }

        // Always sort by FEFO: nearest expiry first!
        sql.append(" ORDER BY b.expiry_date ASC, b.quantity DESC;");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBatch(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Batch findById(int id) {
        String sql = """
            SELECT b.*, m.name as medicine_name, m.generic_name, m.dosage_form, m.strength, 
                   c.name as category_name, s.name as supplier_name
            FROM batches b
            JOIN medicines m ON b.medicine_id = m.id
            LEFT JOIN categories c ON m.category_id = c.id
            LEFT JOIN suppliers s ON b.supplier_id = s.id
            WHERE b.id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBatch(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * FEFO Algorithm: Selects available unexpired, non-quarantined batches for a medicine
     * sorted strictly by expiry_date ASC.
     */
    public List<Batch> findFEFOBatchesForMedicine(int medicineId) {
        List<Batch> list = new ArrayList<>();
        String sql = """
            SELECT b.*, m.name as medicine_name, m.generic_name, m.dosage_form, m.strength, 
                   c.name as category_name, s.name as supplier_name
            FROM batches b
            JOIN medicines m ON b.medicine_id = m.id
            LEFT JOIN categories c ON m.category_id = c.id
            LEFT JOIN suppliers s ON b.supplier_id = s.id
            WHERE b.medicine_id = ? 
              AND b.quantity > 0 
              AND b.status != 'QUARANTINED' 
              AND b.status != 'RECALLED'
              AND b.expiry_date >= date('now')
            ORDER BY b.expiry_date ASC;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBatch(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean create(Batch b) {
        String sql = """
            INSERT INTO batches (medicine_id, batch_number, mfg_date, expiry_date, quantity, 
                                original_quantity, unit_cost, unit_price, discount_percent, 
                                shelf_location, supplier_id, status, notes, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getMedicineId());
            ps.setString(2, b.getBatchNumber());
            ps.setString(3, b.getMfgDate());
            ps.setString(4, b.getExpiryDate());
            ps.setInt(5, b.getQuantity());
            ps.setInt(6, b.getOriginalQuantity() > 0 ? b.getOriginalQuantity() : b.getQuantity());
            ps.setDouble(7, b.getUnitCost());
            ps.setDouble(8, b.getUnitPrice());
            ps.setDouble(9, b.getDiscountPercent());
            ps.setString(10, b.getShelfLocation() != null ? b.getShelfLocation() : "Main Shelf");
            if (b.getSupplierId() > 0) {
                ps.setInt(11, b.getSupplierId());
            } else {
                ps.setNull(11, Types.INTEGER);
            }
            ps.setString(12, b.getStatus() != null ? b.getStatus() : "ACTIVE");
            ps.setString(13, b.getNotes());
            ps.setString(14, DateUtils.now());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) b.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Batch b) {
        String sql = """
            UPDATE batches 
            SET mfg_date = ?, expiry_date = ?, quantity = ?, unit_cost = ?, unit_price = ?, 
                discount_percent = ?, shelf_location = ?, supplier_id = ?, status = ?, notes = ?
            WHERE id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getMfgDate());
            ps.setString(2, b.getExpiryDate());
            ps.setInt(3, b.getQuantity());
            ps.setDouble(4, b.getUnitCost());
            ps.setDouble(5, b.getUnitPrice());
            ps.setDouble(6, b.getDiscountPercent());
            ps.setString(7, b.getShelfLocation());
            if (b.getSupplierId() > 0) {
                ps.setInt(8, b.getSupplierId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            ps.setString(9, b.getStatus());
            ps.setString(10, b.getNotes());
            ps.setInt(11, b.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateDiscount(int batchId, double discountPercent) {
        String sql = "UPDATE batches SET discount_percent = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, discountPercent);
            ps.setInt(2, batchId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean quarantineBatch(int batchId, String notes) {
        String sql = "UPDATE batches SET status = 'QUARANTINED', notes = COALESCE(notes || ' | ', '') || ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "Quarantined on " + DateUtils.today() + ": " + notes);
            ps.setInt(2, batchId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deductQuantity(int batchId, int qtyToDeduct, Connection conn) throws SQLException {
        String sql = "UPDATE batches SET quantity = quantity - ? WHERE id = ? AND quantity >= ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qtyToDeduct);
            ps.setInt(2, batchId);
            ps.setInt(3, qtyToDeduct);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean adjustStock(int batchId, int newQuantity, String reason) {
        String sql = "UPDATE batches SET quantity = ?, notes = COALESCE(notes || ' | ', '') || ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newQuantity);
            ps.setString(2, "Stock adjusted to " + newQuantity + " on " + DateUtils.today() + " Reason: " + reason);
            ps.setInt(3, batchId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM batches WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Batch mapBatch(ResultSet rs) throws SQLException {
        Batch b = new Batch();
        b.setId(rs.getInt("id"));
        b.setMedicineId(rs.getInt("medicine_id"));
        b.setMedicineName(rs.getString("medicine_name"));
        b.setGenericName(rs.getString("generic_name"));
        b.setDosageForm(rs.getString("dosage_form"));
        b.setStrength(rs.getString("strength"));
        b.setCategoryName(rs.getString("category_name"));
        b.setBatchNumber(rs.getString("batch_number"));
        b.setMfgDate(rs.getString("mfg_date"));
        b.setExpiryDate(rs.getString("expiry_date"));
        b.setQuantity(rs.getInt("quantity"));
        b.setOriginalQuantity(rs.getInt("original_quantity"));
        b.setUnitCost(rs.getDouble("unit_cost"));
        b.setUnitPrice(rs.getDouble("unit_price"));
        b.setDiscountPercent(rs.getDouble("discount_percent"));
        b.setShelfLocation(rs.getString("shelf_location"));
        b.setSupplierId(rs.getInt("supplier_id"));
        b.setSupplierName(rs.getString("supplier_name"));
        b.setStatus(rs.getString("status"));
        b.setNotes(rs.getString("notes"));
        b.setCreatedAt(rs.getString("created_at"));
        b.calculateExpiryMetrics();
        return b;
    }
}
