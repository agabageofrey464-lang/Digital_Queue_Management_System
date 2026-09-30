package com.digiq.dao;

import com.digiq.config.Database;
import com.digiq.model.ServiceLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read access to the audit trail. Writes happen inside {@link TokenDAO}'s transactions. */
public class ServiceLogDAO {

    private static final String SELECT =
            "SELECT l.id, l.token_id, l.counter_id, l.staff_id, l.action, l.from_status, l.to_status, "
          + "       l.note, l.created_at, t.token_number, s.name AS service_name, "
          + "       c.name AS counter_name, u.full_name AS staff_name "
          + "FROM service_logs l "
          + "JOIN tokens t ON t.id = l.token_id "
          + "JOIN services s ON s.id = t.service_id "
          + "LEFT JOIN counters c ON c.id = l.counter_id "
          + "LEFT JOIN users u ON u.id = l.staff_id ";

    public List<ServiceLog> findRecent(int limit) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "ORDER BY l.created_at DESC, l.id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        }
    }

    public List<ServiceLog> findByToken(int tokenId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE l.token_id = ? ORDER BY l.created_at")) {
            ps.setInt(1, tokenId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        }
    }

    /** Filtered history for the admin "Service logs" screen. */
    public List<ServiceLog> search(String action, int serviceId, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT + "WHERE 1 = 1 ");
        if (action != null) {
            sql.append("AND l.action = ? ");
        }
        if (serviceId > 0) {
            sql.append("AND s.id = ? ");
        }
        sql.append("ORDER BY l.created_at DESC, l.id DESC LIMIT ?");

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            if (action != null) {
                ps.setString(i++, action);
            }
            if (serviceId > 0) {
                ps.setInt(i++, serviceId);
            }
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        }
    }

    private List<ServiceLog> mapAll(ResultSet rs) throws SQLException {
        List<ServiceLog> list = new ArrayList<>();
        while (rs.next()) {
            ServiceLog l = new ServiceLog();
            l.setId(rs.getInt("id"));
            l.setTokenId(rs.getInt("token_id"));
            int counterId = rs.getInt("counter_id");
            l.setCounterId(rs.wasNull() ? null : counterId);
            int staffId = rs.getInt("staff_id");
            l.setStaffId(rs.wasNull() ? null : staffId);
            l.setAction(rs.getString("action"));
            l.setFromStatus(rs.getString("from_status"));
            l.setToStatus(rs.getString("to_status"));
            l.setNote(rs.getString("note"));
            l.setCreatedAt(rs.getTimestamp("created_at"));
            l.setTokenNumber(rs.getString("token_number"));
            l.setServiceName(rs.getString("service_name"));
            l.setCounterName(rs.getString("counter_name"));
            l.setStaffName(rs.getString("staff_name"));
            list.add(l);
        }
        return list;
    }
}
