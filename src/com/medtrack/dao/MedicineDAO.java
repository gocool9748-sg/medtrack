package com.medtrack.dao;

import com.medtrack.db.DatabaseManager;
import com.medtrack.model.Medicine;
import com.medtrack.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicineDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public List<Medicine> findAll(String search, Integer categoryId, String stockStatus) {
        List<Medicine> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT m.*, c.name as category_name,
                   COALESCE(SUM(CASE WHEN b.quantity > 0 AND b.status != 'EXPIRED' AND b.status != 'QUARANTINED' THEN b.quantity ELSE 0 END), 0) as total_stock,
                   COUNT(CASE WHEN b.quantity > 0 AND b.status != 'EXPIRED' AND b.status != 'QUARANTINED' THEN b.id END) as active_batches,
                   COALESCE(MIN(CASE WHEN b.quantity > 0 THEN b.unit_price END), 0.0) as min_price,
                   COALESCE(MAX(CASE WHEN b.quantity > 0 THEN b.unit_price END), 0.0) as max_price
            FROM medicines m
            LEFT JOIN categories c ON m.category_id = c.id
            LEFT JOIN batches b ON m.id = b.medicine_id
            WHERE m.is_active = 1
        """);

        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (m.name LIKE ? OR m.generic_name LIKE ? OR m.barcode LIKE ? OR m.dosage_form LIKE ?)");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (categoryId != null && categoryId > 0) {
            sql.append(" AND m.category_id = ?");
            params.add(categoryId);
        }

        sql.append(" GROUP BY m.id");

        if ("LOW_STOCK".equalsIgnoreCase(stockStatus)) {
            sql.append(" HAVING total_stock <= m.reorder_level AND total_stock > 0");
        } else if ("OUT_OF_STOCK".equalsIgnoreCase(stockStatus)) {
            sql.append(" HAVING total_stock = 0");
        } else if ("IN_STOCK".equalsIgnoreCase(stockStatus)) {
            sql.append(" HAVING total_stock > m.reorder_level");
        }

        sql.append(" ORDER BY m.name ASC;");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMedicine(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Medicine findById(int id) {
        String sql = """
            SELECT m.*, c.name as category_name,
                   COALESCE(SUM(CASE WHEN b.quantity > 0 AND b.status != 'EXPIRED' AND b.status != 'QUARANTINED' THEN b.quantity ELSE 0 END), 0) as total_stock,
                   COUNT(CASE WHEN b.quantity > 0 AND b.status != 'EXPIRED' AND b.status != 'QUARANTINED' THEN b.id END) as active_batches,
                   COALESCE(MIN(CASE WHEN b.quantity > 0 THEN b.unit_price END), 0.0) as min_price,
                   COALESCE(MAX(CASE WHEN b.quantity > 0 THEN b.unit_price END), 0.0) as max_price
            FROM medicines m
            LEFT JOIN categories c ON m.category_id = c.id
            LEFT JOIN batches b ON m.id = b.medicine_id
            WHERE m.id = ?
            GROUP BY m.id;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMedicine(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Medicine findByBarcode(String barcode) {
        String sql = """
            SELECT m.*, c.name as category_name,
                   COALESCE(SUM(CASE WHEN b.quantity > 0 AND b.status != 'EXPIRED' AND b.status != 'QUARANTINED' THEN b.quantity ELSE 0 END), 0) as total_stock,
                   COUNT(CASE WHEN b.quantity > 0 AND b.status != 'EXPIRED' AND b.status != 'QUARANTINED' THEN b.id END) as active_batches,
                   COALESCE(MIN(CASE WHEN b.quantity > 0 THEN b.unit_price END), 0.0) as min_price,
                   COALESCE(MAX(CASE WHEN b.quantity > 0 THEN b.unit_price END), 0.0) as max_price
            FROM medicines m
            LEFT JOIN categories c ON m.category_id = c.id
            LEFT JOIN batches b ON m.id = b.medicine_id
            WHERE m.barcode = ? AND m.is_active = 1
            GROUP BY m.id;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMedicine(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean create(Medicine m) {
        String sql = """
            INSERT INTO medicines (name, generic_name, category_id, dosage_form, strength, unit, 
                                   reorder_level, min_alert_days, requires_prescription, 
                                   storage_condition, side_effects, barcode, is_active, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?);
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getGenericName());
            ps.setInt(3, m.getCategoryId());
            ps.setString(4, m.getDosageForm());
            ps.setString(5, m.getStrength());
            ps.setString(6, m.getUnit());
            ps.setInt(7, m.getReorderLevel() > 0 ? m.getReorderLevel() : 20);
            ps.setInt(8, m.getMinAlertDays() > 0 ? m.getMinAlertDays() : 60);
            ps.setInt(9, m.isRequiresPrescription() ? 1 : 0);
            ps.setString(10, m.getStorageCondition() != null ? m.getStorageCondition() : "Room Temperature (15-25°C)");
            ps.setString(11, m.getSideEffects());
            ps.setString(12, m.getBarcode() != null && !m.getBarcode().isBlank() ? m.getBarcode() : "MED" + System.currentTimeMillis());
            ps.setString(13, DateUtils.now());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) m.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Medicine m) {
        String sql = """
            UPDATE medicines 
            SET name = ?, generic_name = ?, category_id = ?, dosage_form = ?, strength = ?, unit = ?,
                reorder_level = ?, min_alert_days = ?, requires_prescription = ?, storage_condition = ?,
                side_effects = ?, barcode = ?
            WHERE id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getGenericName());
            ps.setInt(3, m.getCategoryId());
            ps.setString(4, m.getDosageForm());
            ps.setString(5, m.getStrength());
            ps.setString(6, m.getUnit());
            ps.setInt(7, m.getReorderLevel());
            ps.setInt(8, m.getMinAlertDays());
            ps.setInt(9, m.isRequiresPrescription() ? 1 : 0);
            ps.setString(10, m.getStorageCondition());
            ps.setString(11, m.getSideEffects());
            ps.setString(12, m.getBarcode());
            ps.setInt(13, m.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "UPDATE medicines SET is_active = 0 WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Medicine mapMedicine(ResultSet rs) throws SQLException {
        Medicine m = new Medicine();
        m.setId(rs.getInt("id"));
        m.setName(rs.getString("name"));
        m.setGenericName(rs.getString("generic_name"));
        m.setCategoryId(rs.getInt("category_id"));
        m.setCategoryName(rs.getString("category_name"));
        m.setDosageForm(rs.getString("dosage_form"));
        m.setStrength(rs.getString("strength"));
        m.setUnit(rs.getString("unit"));
        m.setReorderLevel(rs.getInt("reorder_level"));
        m.setMinAlertDays(rs.getInt("min_alert_days"));
        m.setRequiresPrescription(rs.getInt("requires_prescription") == 1);
        m.setStorageCondition(rs.getString("storage_condition"));
        m.setSideEffects(rs.getString("side_effects"));
        m.setBarcode(rs.getString("barcode"));
        m.setActive(rs.getInt("is_active") == 1);
        m.setCreatedAt(rs.getString("created_at"));
        m.setTotalStock(rs.getInt("total_stock"));
        m.setActiveBatches(rs.getInt("active_batches"));
        m.setMinPrice(rs.getDouble("min_price"));
        m.setMaxPrice(rs.getDouble("max_price"));
        return m;
    }
}
