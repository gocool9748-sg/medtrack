package com.medtrack.dao;

import com.medtrack.db.DatabaseManager;
import com.medtrack.model.Category;
import com.medtrack.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public List<Category> findAll() {
        List<Category> list = new ArrayList<>();
        String sql = """
            SELECT c.*, COUNT(m.id) as medicine_count 
            FROM categories c 
            LEFT JOIN medicines m ON c.id = m.category_id AND m.is_active = 1
            GROUP BY c.id 
            ORDER BY c.name ASC;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Category cat = new Category();
                cat.setId(rs.getInt("id"));
                cat.setName(rs.getString("name"));
                cat.setDescription(rs.getString("description"));
                cat.setIcon(rs.getString("icon"));
                cat.setMedicineCount(rs.getInt("medicine_count"));
                cat.setCreatedAt(rs.getString("created_at"));
                list.add(cat);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Category findById(int id) {
        String sql = """
            SELECT c.*, COUNT(m.id) as medicine_count 
            FROM categories c 
            LEFT JOIN medicines m ON c.id = m.category_id AND m.is_active = 1
            WHERE c.id = ?
            GROUP BY c.id;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Category cat = new Category();
                    cat.setId(rs.getInt("id"));
                    cat.setName(rs.getString("name"));
                    cat.setDescription(rs.getString("description"));
                    cat.setIcon(rs.getString("icon"));
                    cat.setMedicineCount(rs.getInt("medicine_count"));
                    cat.setCreatedAt(rs.getString("created_at"));
                    return cat;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean create(Category category) {
        String sql = "INSERT INTO categories (name, description, icon, created_at) VALUES (?, ?, ?, ?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setString(3, category.getIcon() != null ? category.getIcon() : "pill");
            ps.setString(4, DateUtils.now());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) category.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Category category) {
        String sql = "UPDATE categories SET name = ?, description = ?, icon = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setString(3, category.getIcon());
            ps.setInt(4, category.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM categories WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
