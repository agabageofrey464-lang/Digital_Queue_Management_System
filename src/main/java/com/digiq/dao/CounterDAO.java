package com.digiq.dao;

import com.digiq.config.Database;
import com.digiq.model.Counter;
import com.digiq.model.CounterStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CounterDAO {

    /** Counter joined with its service, its staff member and whatever it is serving now. */
    private static final String SELECT_FULL =
            "SELECT c.id, c.name, c.service_id, c.staff_id, c.status, c.created_at, "
          + "       s.name AS service_name, s.code AS service_code, u.full_name AS staff_name, "
          + "       (SELECT t.token_number FROM tokens t "
          + "          WHERE t.counter_id = c.id AND t.status = 'IN_SERVICE' "
          + "          ORDER BY t.called_at DESC LIMIT 1) AS current_token, "
          + "       (SELECT COUNT(*) FROM tokens t2 "
          + "          WHERE t2.counter_id = c.id AND t2.service_date = CURDATE() "
          + "            AND t2.status = 'COMPLETED') AS served_today "
          + "FROM counters c "
          + "JOIN services s ON s.id = c.service_id "
          + "LEFT JOIN users u ON u.id = c.staff_id ";

    public List<Counter> findAll() throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_FULL + "ORDER BY c.name");
             ResultSet rs = ps.executeQuery()) {
            return mapAll(rs);
        }
    }

    /** Counters that are OPEN or PAUSED - what the public display board shows. */
    public List<Counter> findServing() throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     SELECT_FULL + "WHERE c.status IN ('OPEN','PAUSED') ORDER BY c.name");
             ResultSet rs = ps.executeQuery()) {
            return mapAll(rs);
        }
    }

    public Counter findById(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_FULL + "WHERE c.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** The counter a staff member is signed in to, if any. */
    public Counter findByStaffId(int staffId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_FULL + "WHERE c.staff_id = ? LIMIT 1")) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int countOpen() throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM counters WHERE status = 'OPEN'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int insert(Counter counter) throws SQLException {
        String sql = "INSERT INTO counters (name, service_id, staff_id, status) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, counter.getName());
            ps.setInt(2, counter.getServiceId());
            setNullableInt(ps, 3, counter.getStaffId());
            ps.setString(4, counter.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    counter.setId(keys.getInt(1));
                }
            }
            return counter.getId();
        }
    }

    public void update(Counter counter) throws SQLException {
        String sql = "UPDATE counters SET name = ?, service_id = ?, staff_id = ?, status = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, counter.getName());
            ps.setInt(2, counter.getServiceId());
            setNullableInt(ps, 3, counter.getStaffId());
            ps.setString(4, counter.getStatus().name());
            ps.setInt(5, counter.getId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(int counterId, CounterStatus status) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE counters SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, counterId);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM counters WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private List<Counter> mapAll(ResultSet rs) throws SQLException {
        List<Counter> list = new ArrayList<>();
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    private Counter map(ResultSet rs) throws SQLException {
        Counter c = new Counter();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setServiceId(rs.getInt("service_id"));
        int staffId = rs.getInt("staff_id");
        c.setStaffId(rs.wasNull() ? null : staffId);
        c.setStatus(CounterStatus.from(rs.getString("status")));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setServiceName(rs.getString("service_name"));
        c.setServiceCode(rs.getString("service_code"));
        c.setStaffName(rs.getString("staff_name"));
        c.setCurrentTokenNumber(rs.getString("current_token"));
        c.setServedToday(rs.getInt("served_today"));
        return c;
    }
}
