package com.digiq.dao;

import com.digiq.config.Database;
import com.digiq.model.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceDAO {

    private static final String SELECT =
            "SELECT id, name, code, description, avg_service_minutes, active, created_at FROM services ";

    public List<Service> findAll() throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            List<Service> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        }
    }

    public Service findById(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean codeExists(String code, int excludeId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM services WHERE code = ? AND id <> ?")) {
            ps.setString(1, code);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Active services with today's queue depth and the number of counters currently
     * serving them - everything the "book a token" cards and the display board need.
     */
    public List<Service> findActiveWithQueueStats() throws SQLException {
        String sql =
                "SELECT s.id, s.name, s.code, s.description, s.avg_service_minutes, s.active, s.created_at, "
              + "  (SELECT COUNT(*) FROM tokens t "
              + "     WHERE t.service_id = s.id AND t.service_date = CURDATE() AND t.status = 'PENDING') AS waiting, "
              + "  (SELECT COUNT(*) FROM counters ct "
              + "     WHERE ct.service_id = s.id AND ct.status = 'OPEN') AS open_counters "
              + "FROM services s WHERE s.active = 1 ORDER BY s.name";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Service> list = new ArrayList<>();
            while (rs.next()) {
                Service s = map(rs);
                s.setWaitingCount(rs.getInt("waiting"));
                s.setOpenCounters(rs.getInt("open_counters"));
                list.add(s);
            }
            return list;
        }
    }

    public int insert(Service service) throws SQLException {
        String sql = "INSERT INTO services (name, code, description, avg_service_minutes, active) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, service.getName());
            ps.setString(2, service.getCode());
            ps.setString(3, service.getDescription());
            ps.setInt(4, service.getAvgServiceMinutes());
            ps.setBoolean(5, service.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    service.setId(keys.getInt(1));
                }
            }
            return service.getId();
        }
    }

    public void update(Service service) throws SQLException {
        String sql = "UPDATE services SET name = ?, code = ?, description = ?, "
                + "avg_service_minutes = ?, active = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, service.getName());
            ps.setString(2, service.getCode());
            ps.setString(3, service.getDescription());
            ps.setInt(4, service.getAvgServiceMinutes());
            ps.setBoolean(5, service.isActive());
            ps.setInt(6, service.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM services WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Service map(ResultSet rs) throws SQLException {
        Service s = new Service();
        s.setId(rs.getInt("id"));
        s.setName(rs.getString("name"));
        s.setCode(rs.getString("code"));
        s.setDescription(rs.getString("description"));
        s.setAvgServiceMinutes(rs.getInt("avg_service_minutes"));
        s.setActive(rs.getBoolean("active"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        return s;
    }
}
