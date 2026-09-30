package com.digiq.dao;

import com.digiq.config.Database;
import com.digiq.model.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    private static final String SELECT =
            "SELECT n.id, n.user_id, n.token_id, n.title, n.message, n.type, n.read_flag, n.created_at, "
          + "       t.token_number "
          + "FROM notifications n LEFT JOIN tokens t ON t.id = n.token_id ";

    public void insert(int userId, Integer tokenId, String title, String message, String type)
            throws SQLException {
        String sql = "INSERT INTO notifications (user_id, token_id, title, message, type) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (tokenId == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, tokenId);
            }
            ps.setString(3, title);
            ps.setString(4, message);
            ps.setString(5, type);
            ps.executeUpdate();
        }
    }

    public List<Notification> findByUser(int userId, int limit) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     SELECT + "WHERE n.user_id = ? ORDER BY n.created_at DESC LIMIT ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Notification> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    public int countUnread(int userId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_flag = 0")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void markAllRead(int userId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE notifications SET read_flag = 1 WHERE user_id = ? AND read_flag = 0")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * True when an alert of this type was already raised for this token, so the
     * "you are next" warning is written once rather than on every queue change.
     */
    public boolean exists(int userId, int tokenId, String type) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM notifications WHERE user_id = ? AND token_id = ? AND type = ? LIMIT 1")) {
            ps.setInt(1, userId);
            ps.setInt(2, tokenId);
            ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        int tokenId = rs.getInt("token_id");
        n.setTokenId(rs.wasNull() ? null : tokenId);
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setRead(rs.getBoolean("read_flag"));
        n.setCreatedAt(rs.getTimestamp("created_at"));
        n.setTokenNumber(rs.getString("token_number"));
        return n;
    }
}
