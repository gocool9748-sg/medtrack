package com.medtrack.dao;

import com.medtrack.db.DatabaseManager;
import com.medtrack.model.DisposalRecord;
import com.medtrack.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DisposalDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public boolean create(DisposalRecord d) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            if (d.getCertificateNumber() == null || d.getCertificateNumber().isBlank()) {
                d.setCertificateNumber("DISP-CERT-" + (System.currentTimeMillis() % 10000000));
            }
            if (d.getDisposalDate() == null || d.getDisposalDate().isBlank()) {
                d.setDisposalDate(DateUtils.today());
            }

            String sql = """
                INSERT INTO disposals (batch_id, medicine_id, batch_number, quantity, unit_cost, 
                                       total_loss, reason, destruction_method, authorized_by, 
                                       disposal_date, certificate_number, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                if (d.getBatchId() > 0) {
                    ps.setInt(1, d.getBatchId());
                } else {
                    ps.setNull(1, Types.INTEGER);
                }
                ps.setInt(2, d.getMedicineId());
                ps.setString(3, d.getBatchNumber());
                ps.setInt(4, d.getQuantity());
                ps.setDouble(5, d.getUnitCost());
                ps.setDouble(6, d.getTotalLoss());
                ps.setString(7, d.getReason());
                ps.setString(8, d.getDestructionMethod());
                if (d.getAuthorizedBy() > 0) {
                    ps.setInt(9, d.getAuthorizedBy());
                } else {
                    ps.setNull(9, Types.INTEGER);
                }
                ps.setString(10, d.getDisposalDate());
                ps.setString(11, d.getCertificateNumber());
                ps.setString(12, d.getNotes());

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) d.setId(rs.getInt(1));
                }
            }

            // Also update the batch quantity to 0 and status to QUARANTINED/EXPIRED if batchId exists
            if (d.getBatchId() > 0) {
                String updateBatchSql = "UPDATE batches SET quantity = 0, status = 'DISPOSED', notes = COALESCE(notes || ' | ', '') || ? WHERE id = ?;";
                try (PreparedStatement psBatch = conn.prepareStatement(updateBatchSql)) {
                    psBatch.setString(1, "Disposed on " + d.getDisposalDate() + " Cert: " + d.getCertificateNumber());
                    psBatch.setInt(2, d.getBatchId());
                    psBatch.executeUpdate();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    public List<DisposalRecord> findAll(String search, String reason) {
        List<DisposalRecord> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT d.*, m.name as medicine_name, m.generic_name, u.full_name as authorizer_name
            FROM disposals d
            JOIN medicines m ON d.medicine_id = m.id
            LEFT JOIN users u ON d.authorized_by = u.id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (d.batch_number LIKE ? OR m.name LIKE ? OR d.certificate_number LIKE ?)");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (reason != null && !reason.isBlank() && !"ALL".equalsIgnoreCase(reason)) {
            sql.append(" AND d.reason = ?");
            params.add(reason.toUpperCase());
        }

        sql.append(" ORDER BY d.disposal_date DESC, d.id DESC;");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DisposalRecord d = new DisposalRecord();
                    d.setId(rs.getInt("id"));
                    d.setBatchId(rs.getInt("batch_id"));
                    d.setMedicineId(rs.getInt("medicine_id"));
                    d.setMedicineName(rs.getString("medicine_name"));
                    d.setGenericName(rs.getString("generic_name"));
                    d.setBatchNumber(rs.getString("batch_number"));
                    d.setQuantity(rs.getInt("quantity"));
                    d.setUnitCost(rs.getDouble("unit_cost"));
                    d.setTotalLoss(rs.getDouble("total_loss"));
                    d.setReason(rs.getString("reason"));
                    d.setDestructionMethod(rs.getString("destruction_method"));
                    d.setAuthorizedBy(rs.getInt("authorized_by"));
                    d.setAuthorizerName(rs.getString("authorizer_name"));
                    d.setDisposalDate(rs.getString("disposal_date"));
                    d.setCertificateNumber(rs.getString("certificate_number"));
                    d.setNotes(rs.getString("notes"));
                    list.add(d);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
