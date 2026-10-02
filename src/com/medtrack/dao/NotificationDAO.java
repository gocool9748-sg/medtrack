package com.medtrack.dao;

import com.medtrack.db.DatabaseManager;
import com.medtrack.model.Notification;
import com.medtrack.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public List<Notification> findAll(Boolean unreadOnly, Integer limit) {
        List<Notification> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM notifications WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (Boolean.TRUE.equals(unreadOnly)) {
            sql.append(" AND is_read = 0");
        }

        sql.append(" ORDER BY is_read ASC, id DESC");

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
                    list.add(mapNotification(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getUnreadCount() {
        String sql = "SELECT COUNT(*) FROM notifications WHERE is_read = 0;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean markAsRead(int id) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean markAllAsRead() {
        String sql = "UPDATE notifications SET is_read = 1 WHERE is_read = 0;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean create(Notification notif) {
        String sql = "INSERT INTO notifications (title, message, type, severity, is_read, link_type, link_id, created_at) VALUES (?, ?, ?, ?, 0, ?, ?, ?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, notif.getTitle());
            ps.setString(2, notif.getMessage());
            ps.setString(3, notif.getType() != null ? notif.getType() : "INFO");
            ps.setString(4, notif.getSeverity() != null ? notif.getSeverity() : "INFO");
            ps.setString(5, notif.getLinkType());
            ps.setString(6, notif.getLinkId());
            ps.setString(7, DateUtils.now());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) notif.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setSeverity(rs.getString("severity"));
        n.setRead(rs.getInt("is_read") == 1);
        n.setLinkType(rs.getString("link_type"));
        n.setLinkId(rs.getString("link_id"));
        n.setCreatedAt(rs.getString("created_at"));
        return n;
    }
}
