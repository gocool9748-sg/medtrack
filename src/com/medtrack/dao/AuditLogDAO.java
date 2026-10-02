package com.medtrack.dao;

import com.medtrack.db.DatabaseManager;
import com.medtrack.model.AuditLog;
import com.medtrack.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public void log(Integer userId, String username, String role, String action, String entityType, String entityId, String details, String ipAddress) {
        String sql = """
            INSERT INTO audit_logs (user_id, username, role, action, entity_type, entity_id, details, ip_address, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null && userId > 0) {
                ps.setInt(1, userId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, username != null ? username : "SYSTEM");
            ps.setString(3, role != null ? role : "SYSTEM");
            ps.setString(4, action);
            ps.setString(5, entityType != null ? entityType : "SYSTEM");
            ps.setString(6, entityId);
            ps.setString(7, details);
            ps.setString(8, ipAddress != null ? ipAddress : "127.0.0.1");
            ps.setString(9, DateUtils.now());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }

    public List<AuditLog> findAll(String search, String actionFilter, Integer limit) {
        List<AuditLog> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (username LIKE ? OR action LIKE ? OR details LIKE ? OR entity_type LIKE ? OR ip_address LIKE ?)");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (actionFilter != null && !actionFilter.isBlank() && !"ALL".equalsIgnoreCase(actionFilter)) {
            sql.append(" AND action = ?");
            params.add(actionFilter.toUpperCase());
        }

        sql.append(" ORDER BY id DESC");

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
                    AuditLog log = new AuditLog();
                    log.setId(rs.getInt("id"));
                    log.setUserId(rs.getInt("user_id"));
                    log.setUsername(rs.getString("username"));
                    log.setRole(rs.getString("role"));
                    log.setAction(rs.getString("action"));
                    log.setEntityType(rs.getString("entity_type"));
                    log.setEntityId(rs.getString("entity_id"));
                    log.setDetails(rs.getString("details"));
                    log.setIpAddress(rs.getString("ip_address"));
                    log.setCreatedAt(rs.getString("created_at"));
                    list.add(log);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
