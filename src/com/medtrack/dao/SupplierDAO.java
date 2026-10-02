package com.medtrack.dao;

import com.medtrack.db.DatabaseManager;
import com.medtrack.model.Supplier;
import com.medtrack.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public List<Supplier> findAll() {
        List<Supplier> list = new ArrayList<>();
        String sql = """
            SELECT s.*, COUNT(po.id) as total_orders
            FROM suppliers s
            LEFT JOIN purchase_orders po ON s.id = po.supplier_id
            GROUP BY s.id
            ORDER BY s.name ASC;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapSupplier(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Supplier findById(int id) {
        String sql = """
            SELECT s.*, COUNT(po.id) as total_orders
            FROM suppliers s
            LEFT JOIN purchase_orders po ON s.id = po.supplier_id
            WHERE s.id = ?
            GROUP BY s.id;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSupplier(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean create(Supplier s) {
        String sql = "INSERT INTO suppliers (name, contact_person, email, phone, address, tax_id, rating, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactPerson());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getPhone());
            ps.setString(5, s.getAddress());
            ps.setString(6, s.getTaxId());
            ps.setDouble(7, s.getRating() > 0 ? s.getRating() : 5.0);
            ps.setString(8, DateUtils.now());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) s.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Supplier s) {
        String sql = "UPDATE suppliers SET name = ?, contact_person = ?, email = ?, phone = ?, address = ?, tax_id = ?, rating = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactPerson());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getPhone());
            ps.setString(5, s.getAddress());
            ps.setString(6, s.getTaxId());
            ps.setDouble(7, s.getRating());
            ps.setInt(8, s.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM suppliers WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Supplier mapSupplier(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getInt("id"));
        s.setName(rs.getString("name"));
        s.setContactPerson(rs.getString("contact_person"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        s.setAddress(rs.getString("address"));
        s.setTaxId(rs.getString("tax_id"));
        s.setRating(rs.getDouble("rating"));
        s.setTotalOrders(rs.getInt("total_orders"));
        s.setCreatedAt(rs.getString("created_at"));
        return s;
    }
}
