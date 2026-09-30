package com.digiq.dao;

import com.digiq.config.Database;
import com.digiq.model.DashboardStats;
import com.digiq.util.Json;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Read-only aggregate queries behind the dashboard tiles and the analytics charts.
 *
 * <p>Every series is returned as a list of plain maps so it can be handed straight to
 * Gson and consumed by Chart.js without an intermediate DTO per chart.</p>
 */
public class AnalyticsDAO {

    /** Everything on the dashboard tiles, in one round trip per figure. */
    public DashboardStats dashboard() throws SQLException {
        DashboardStats stats = new DashboardStats();

        String today = "SELECT "
                + "  COUNT(*) AS issued, "
                + "  SUM(status = 'COMPLETED') AS completed, "
                + "  SUM(status = 'PENDING') AS waiting, "
                + "  SUM(status = 'IN_SERVICE') AS in_service, "
                + "  SUM(status = 'NO_SHOW') AS no_show, "
                + "  AVG(CASE WHEN called_at IS NOT NULL "
                + "           THEN TIMESTAMPDIFF(SECOND, issued_at, called_at) / 60 END) AS avg_wait, "
                + "  AVG(CASE WHEN completed_at IS NOT NULL "
                + "           THEN TIMESTAMPDIFF(SECOND, called_at, completed_at) / 60 END) AS avg_service "
                + "FROM tokens WHERE service_date = CURDATE()";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(today);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.setIssuedToday(rs.getInt("issued"));
                stats.setCompletedToday(rs.getInt("completed"));
                stats.setWaitingNow(rs.getInt("waiting"));
                stats.setInServiceNow(rs.getInt("in_service"));
                stats.setNoShowToday(rs.getInt("no_show"));
                stats.setAvgWaitMinutes(round1(rs.getDouble("avg_wait")));
                stats.setAvgServiceMinutes(round1(rs.getDouble("avg_service")));
            }
        }

        String counts = "SELECT "
                + "  (SELECT COUNT(*) FROM counters) AS counters, "
                + "  (SELECT COUNT(*) FROM counters WHERE status = 'OPEN') AS open_counters, "
                + "  (SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER') AS customers, "
                + "  (SELECT COUNT(*) FROM users WHERE role = 'STAFF') AS staff, "
                + "  (SELECT COUNT(*) FROM services WHERE active = 1) AS services";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(counts);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.setTotalCounters(rs.getInt("counters"));
                stats.setOpenCounters(rs.getInt("open_counters"));
                stats.setTotalCustomers(rs.getInt("customers"));
                stats.setTotalStaff(rs.getInt("staff"));
                stats.setTotalServices(rs.getInt("services"));
            }
        }
        return stats;
    }

    /**
     * Tokens issued vs completed per day. Driven off a generated date spine so days
     * with no activity appear as zeroes instead of dropping out of the line.
     */
    public List<Map<String, Object>> tokensPerDay(int days) throws SQLException {
        String sql = "SELECT d.day AS day, "
                + "       COALESCE(SUM(t.id IS NOT NULL), 0) AS issued, "
                + "       COALESCE(SUM(t.status = 'COMPLETED'), 0) AS completed "
                + "FROM ( SELECT CURDATE() - INTERVAL seq DAY AS day "
                + "       FROM ( SELECT a.n + b.n * 10 AS seq FROM "
                + "                (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 "
                + "                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a "
                + "                CROSS JOIN "
                + "                (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) b ) nums "
                + "       WHERE seq < ? ) d "
                + "LEFT JOIN tokens t ON t.service_date = d.day "
                + "GROUP BY d.day ORDER BY d.day";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(Json.map(
                            "date", rs.getDate("day").toString(),
                            "issued", rs.getInt("issued"),
                            "completed", rs.getInt("completed")));
                }
                return rows;
            }
        }
    }

    /** How the last N days of tokens ended up. */
    public List<Map<String, Object>> statusBreakdown(int days) throws SQLException {
        String sql = "SELECT status, COUNT(*) AS n FROM tokens "
                + "WHERE service_date >= CURDATE() - INTERVAL ? DAY GROUP BY status ORDER BY n DESC";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(Json.map("status", rs.getString("status"), "count", rs.getInt("n")));
                }
                return rows;
            }
        }
    }

    /** Average minutes waited vs minutes at the counter, per service. */
    public List<Map<String, Object>> waitVsServiceByService(int days) throws SQLException {
        String sql = "SELECT s.name AS service, s.code AS code, "
                + "       AVG(CASE WHEN t.called_at IS NOT NULL "
                + "                THEN TIMESTAMPDIFF(SECOND, t.issued_at, t.called_at) / 60 END) AS avg_wait, "
                + "       AVG(CASE WHEN t.completed_at IS NOT NULL "
                + "                THEN TIMESTAMPDIFF(SECOND, t.called_at, t.completed_at) / 60 END) AS avg_service "
                + "FROM services s LEFT JOIN tokens t "
                + "  ON t.service_id = s.id AND t.service_date >= CURDATE() - INTERVAL ? DAY "
                + "GROUP BY s.id, s.name, s.code ORDER BY s.name";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(Json.map(
                            "service", rs.getString("service"),
                            "code", rs.getString("code"),
                            "avgWait", round1(rs.getDouble("avg_wait")),
                            "avgService", round1(rs.getDouble("avg_service"))));
                }
                return rows;
            }
        }
    }

    /** Arrivals by hour of day - shows where the crowd actually forms. */
    public List<Map<String, Object>> peakHours(int days) throws SQLException {
        String sql = "SELECT HOUR(issued_at) AS h, COUNT(*) AS n FROM tokens "
                + "WHERE service_date >= CURDATE() - INTERVAL ? DAY GROUP BY h ORDER BY h";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    int h = rs.getInt("h");
                    rows.add(Json.map(
                            "hour", h,
                            "label", String.format("%02d:00", h),
                            "count", rs.getInt("n")));
                }
                return rows;
            }
        }
    }

    /** Throughput per counter. */
    public List<Map<String, Object>> counterPerformance(int days) throws SQLException {
        String sql = "SELECT c.name AS counter, s.name AS service, "
                + "       COUNT(t.id) AS served, "
                + "       AVG(CASE WHEN t.completed_at IS NOT NULL "
                + "                THEN TIMESTAMPDIFF(SECOND, t.called_at, t.completed_at) / 60 END) AS avg_service "
                + "FROM counters c "
                + "JOIN services s ON s.id = c.service_id "
                + "LEFT JOIN tokens t ON t.counter_id = c.id "
                + "     AND t.status = 'COMPLETED' AND t.service_date >= CURDATE() - INTERVAL ? DAY "
                + "GROUP BY c.id, c.name, s.name ORDER BY served DESC";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(Json.map(
                            "counter", rs.getString("counter"),
                            "service", rs.getString("service"),
                            "served", rs.getInt("served"),
                            "avgService", round1(rs.getDouble("avg_service"))));
                }
                return rows;
            }
        }
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
