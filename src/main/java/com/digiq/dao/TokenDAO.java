package com.digiq.dao;

import com.digiq.config.Database;
import com.digiq.model.Token;
import com.digiq.model.TokenStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TokenDAO {

    /** Token joined with everything the queue screens display. */
    private static final String SELECT_FULL =
            "SELECT t.id, t.token_number, t.qr_payload, t.service_id, t.customer_id, t.counter_id, "
          + "       t.status, t.priority, t.service_date, t.issued_at, t.called_at, t.completed_at, "
          + "       s.name AS service_name, s.code AS service_code, s.avg_service_minutes, "
          + "       u.full_name AS customer_name, u.phone AS customer_phone, c.name AS counter_name "
          + "FROM tokens t "
          + "JOIN services s ON s.id = t.service_id "
          + "JOIN users u ON u.id = t.customer_id "
          + "LEFT JOIN counters c ON c.id = t.counter_id ";

    /** How many times a transaction is re-run after InnoDB rolls it back. */
    private static final int MAX_ATTEMPTS = 4;

    // ------------------------------------------------------------------
    //  Deadlock handling
    //
    //  Two counters calling the next customer at the same moment both update
    //  `tokens`, and both touch idx_token_queue as the status changes. InnoDB
    //  will sometimes pick one of them as a deadlock victim and roll it back.
    //  That is normal, expected behaviour, not a fault: MySQL's own guidance is
    //  that an application must be ready to reissue the transaction. Without
    //  this, a busy branch shows staff a spurious "could not reach the
    //  database" every so often, and a waiting customer is silently skipped.
    // ------------------------------------------------------------------

    @FunctionalInterface
    private interface TxWork<T> {
        T run() throws SQLException;
    }

    private <T> T withRetry(TxWork<T> work) throws SQLException {
        SQLException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return work.run();
            } catch (SQLException ex) {
                if (!isRetryable(ex) || attempt == MAX_ATTEMPTS) {
                    throw ex;
                }
                last = ex;
                try {
                    // Back off a little, and by a different amount each attempt,
                    // so the two transactions do not simply collide again.
                    Thread.sleep(15L * attempt + (long) (Math.random() * 15));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw last;
    }

    /** Deadlock (1213) and lock-wait timeout (1205) are both worth another try. */
    private boolean isRetryable(SQLException ex) {
        if (ex instanceof SQLTransactionRollbackException) {
            return true;
        }
        int code = ex.getErrorCode();
        return code == 1213 || code == 1205 || "40001".equals(ex.getSQLState());
    }

    // ------------------------------------------------------------------
    //  Issuing
    // ------------------------------------------------------------------

    /**
     * Issues the next token for a service, today.
     *
     * <p>The per-service daily sequence is read and the row inserted inside one
     * transaction, with the day's existing rows locked, so two customers booking at
     * the same instant cannot be handed the same number. The unique key
     * {@code uq_token_day} is the backstop if they somehow do.</p>
     */
    public Token issue(int serviceId, int customerId, boolean priority) throws SQLException {
        return withRetry(() -> issueOnce(serviceId, customerId, priority));
    }

    private Token issueOnce(int serviceId, int customerId, boolean priority) throws SQLException {
        Connection c = null;
        try {
            c = Database.getConnection();
            c.setAutoCommit(false);

            String code;
            int avgMinutes;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT code, avg_service_minutes FROM services WHERE id = ?")) {
                ps.setInt(1, serviceId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Unknown service id " + serviceId);
                    }
                    code = rs.getString("code");
                    avgMinutes = rs.getInt("avg_service_minutes");
                }
            }

            int next;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM tokens WHERE service_id = ? AND service_date = CURDATE() FOR UPDATE")) {
                ps.setInt(1, serviceId);
                try (ResultSet rs = ps.executeQuery()) {
                    next = rs.next() ? rs.getInt(1) + 1 : 1;
                }
            }

            String tokenNumber = String.format("%s-%04d", code, next);
            String qrPayload = UUID.randomUUID().toString();

            String insert = "INSERT INTO tokens (token_number, qr_payload, service_id, customer_id, "
                    + "status, priority, service_date) VALUES (?, ?, ?, ?, 'PENDING', ?, CURDATE())";
            int newId;
            try (PreparedStatement ps = c.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, tokenNumber);
                ps.setString(2, qrPayload);
                ps.setInt(3, serviceId);
                ps.setInt(4, customerId);
                ps.setBoolean(5, priority);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    newId = keys.next() ? keys.getInt(1) : 0;
                }
            }

            log(c, newId, null, null, "ISSUED", null, "PENDING", "Token booked by customer");
            c.commit();

            Token token = findById(newId);
            if (token != null) {
                token.setAvgServiceMinutes(avgMinutes);
            }
            return token;
        } catch (SQLException ex) {
            rollback(c);
            throw ex;
        } finally {
            close(c);
        }
    }

    // ------------------------------------------------------------------
    //  Serving
    // ------------------------------------------------------------------

    /**
     * Moves the longest-waiting token for this counter's service into IN_SERVICE and
     * returns it, or null when the queue is empty.
     *
     * <p>{@code FOR UPDATE SKIP LOCKED} is what makes this safe with several counters
     * calling at once: each transaction locks a different row instead of blocking, so
     * two staff members pressing "Call next" together get two different customers.
     * Priority tokens jump the queue; otherwise it is first-come, first-served.</p>
     *
     * <p><strong>This depends on {@code idx_token_queue} being ordered
     * {@code (..., priority DESC, issued_at ASC)}</strong> - the same order as the
     * {@code ORDER BY} below. Against a plain ascending index the mixed ordering needs
     * a filesort, which reads and locks every matching row before returning one; the
     * second counter then skips the entire queue and is wrongly told nobody is
     * waiting. If you change that index, change this query with it.</p>
     */
    public Token callNext(int counterId, int serviceId, Integer staffId) throws SQLException {
        return withRetry(() -> callNextOnce(counterId, serviceId, staffId));
    }

    private Token callNextOnce(int counterId, int serviceId, Integer staffId) throws SQLException {
        Connection c = null;
        try {
            c = Database.getConnection();
            c.setAutoCommit(false);

            Integer tokenId = null;
            String sql = "SELECT id FROM tokens "
                    + "WHERE service_id = ? AND service_date = CURDATE() AND status = 'PENDING' "
                    + "ORDER BY priority DESC, issued_at ASC LIMIT 1 FOR UPDATE SKIP LOCKED";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, serviceId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tokenId = rs.getInt(1);
                    }
                }
            }

            if (tokenId == null) {
                c.rollback();
                return null;
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE tokens SET status = 'IN_SERVICE', counter_id = ?, called_at = NOW() WHERE id = ?")) {
                ps.setInt(1, counterId);
                ps.setInt(2, tokenId);
                ps.executeUpdate();
            }

            log(c, tokenId, counterId, staffId, "CALLED", "PENDING", "IN_SERVICE", null);
            c.commit();
            return findById(tokenId);
        } catch (SQLException ex) {
            rollback(c);
            throw ex;
        } finally {
            close(c);
        }
    }

    /** Applies a terminal status (COMPLETED / NO_SHOW / CANCELLED) and writes the audit row. */
    public Token changeStatus(int tokenId, TokenStatus to, Integer counterId, Integer staffId, String note)
            throws SQLException {
        return withRetry(() -> changeStatusOnce(tokenId, to, counterId, staffId, note));
    }

    private Token changeStatusOnce(int tokenId, TokenStatus to, Integer counterId, Integer staffId, String note)
            throws SQLException {
        Connection c = null;
        try {
            c = Database.getConnection();
            c.setAutoCommit(false);

            String from;
            try (PreparedStatement ps = c.prepareStatement("SELECT status FROM tokens WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, tokenId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        return null;
                    }
                    from = rs.getString("status");
                }
            }

            String sql = (to == TokenStatus.COMPLETED)
                    ? "UPDATE tokens SET status = ?, completed_at = NOW() WHERE id = ?"
                    : "UPDATE tokens SET status = ? WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, to.name());
                ps.setInt(2, tokenId);
                ps.executeUpdate();
            }

            log(c, tokenId, counterId, staffId, to.name(), from, to.name(), note);
            c.commit();
            return findById(tokenId);
        } catch (SQLException ex) {
            rollback(c);
            throw ex;
        } finally {
            close(c);
        }
    }

    // ------------------------------------------------------------------
    //  Lookups
    // ------------------------------------------------------------------

    public Token findById(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_FULL + "WHERE t.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Resolves the UUID encoded in a printed QR code. */
    public Token findByQrPayload(String payload) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_FULL + "WHERE t.qr_payload = ?")) {
            ps.setString(1, payload);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Accepts either the scanned UUID or a typed token number such as ACC-0042. */
    public Token findByQrOrNumber(String value) throws SQLException {
        Token token = findByQrPayload(value);
        if (token != null) {
            return token;
        }
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     SELECT_FULL + "WHERE t.token_number = ? AND t.service_date = CURDATE() LIMIT 1")) {
            ps.setString(1, value.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Token findActiveByCounter(int counterId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     SELECT_FULL + "WHERE t.counter_id = ? AND t.status = 'IN_SERVICE' "
                             + "ORDER BY t.called_at DESC LIMIT 1")) {
            ps.setInt(1, counterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Token> findByCustomer(int customerId, int limit) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     SELECT_FULL + "WHERE t.customer_id = ? ORDER BY t.issued_at DESC LIMIT ?")) {
            ps.setInt(1, customerId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        }
    }

    /** A customer's still-live tokens for today - what the tracking page watches. */
    public List<Token> findActiveByCustomer(int customerId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     SELECT_FULL + "WHERE t.customer_id = ? AND t.service_date = CURDATE() "
                             + "AND t.status IN ('PENDING','IN_SERVICE') ORDER BY t.issued_at")) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        }
    }

    /** Today's waiting queue for one service, in the order it will be served. */
    public List<Token> findPendingQueue(int serviceId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     SELECT_FULL + "WHERE t.service_id = ? AND t.service_date = CURDATE() "
                             + "AND t.status = 'PENDING' ORDER BY t.priority DESC, t.issued_at ASC")) {
            ps.setInt(1, serviceId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        }
    }

    /** The most recently called tokens - the "Now Serving" rows on the display board. */
    public List<Token> findRecentlyCalled(int limit) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     SELECT_FULL + "WHERE t.service_date = CURDATE() AND t.called_at IS NOT NULL "
                             + "ORDER BY t.called_at DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        }
    }

    public List<Token> findToday(TokenStatus status, int serviceId, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_FULL + "WHERE t.service_date = CURDATE() ");
        if (status != null) {
            sql.append("AND t.status = ? ");
        }
        if (serviceId > 0) {
            sql.append("AND t.service_id = ? ");
        }
        sql.append("ORDER BY t.issued_at DESC LIMIT ?");

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            if (status != null) {
                ps.setString(i++, status.name());
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

    /** 1 = next to be served. 0 when the token is not waiting any more. */
    public int positionOf(Token token) throws SQLException {
        if (token.getStatus() != TokenStatus.PENDING) {
            return 0;
        }
        String sql = "SELECT COUNT(*) + 1 FROM tokens "
                + "WHERE service_id = ? AND service_date = CURDATE() AND status = 'PENDING' "
                + "AND (priority > ? OR (priority = ? AND issued_at < ?))";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int p = token.isPriority() ? 1 : 0;
            ps.setInt(1, token.getServiceId());
            ps.setInt(2, p);
            ps.setInt(3, p);
            ps.setTimestamp(4, token.getIssuedAt());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countWaiting(int serviceId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tokens WHERE service_date = CURDATE() AND status = 'PENDING'"
                + (serviceId > 0 ? " AND service_id = ?" : "");
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (serviceId > 0) {
                ps.setInt(1, serviceId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ------------------------------------------------------------------
    //  Internals
    // ------------------------------------------------------------------

    private void log(Connection c, int tokenId, Integer counterId, Integer staffId,
                     String action, String from, String to, String note) throws SQLException {
        String sql = "INSERT INTO service_logs (token_id, counter_id, staff_id, action, from_status, to_status, note) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, tokenId);
            if (counterId == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, counterId);
            }
            if (staffId == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, staffId);
            }
            ps.setString(4, action);
            ps.setString(5, from);
            ps.setString(6, to);
            ps.setString(7, note);
            ps.executeUpdate();
        }
    }

    private void rollback(Connection c) {
        if (c != null) {
            try {
                c.rollback();
            } catch (SQLException ignored) {
                // Nothing useful to do - the original exception is the one that matters.
            }
        }
    }

    private void close(Connection c) {
        if (c != null) {
            try {
                c.setAutoCommit(true);
                c.close();
            } catch (SQLException ignored) {
                // Returning the connection to the pool is best-effort.
            }
        }
    }

    private List<Token> mapAll(ResultSet rs) throws SQLException {
        List<Token> list = new ArrayList<>();
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    private Token map(ResultSet rs) throws SQLException {
        Token t = new Token();
        t.setId(rs.getInt("id"));
        t.setTokenNumber(rs.getString("token_number"));
        t.setQrPayload(rs.getString("qr_payload"));
        t.setServiceId(rs.getInt("service_id"));
        t.setCustomerId(rs.getInt("customer_id"));
        int counterId = rs.getInt("counter_id");
        t.setCounterId(rs.wasNull() ? null : counterId);
        t.setStatus(TokenStatus.from(rs.getString("status")));
        t.setPriority(rs.getInt("priority") == 1);
        t.setServiceDate(rs.getDate("service_date"));
        t.setIssuedAt(rs.getTimestamp("issued_at"));
        t.setCalledAt(rs.getTimestamp("called_at"));
        t.setCompletedAt(rs.getTimestamp("completed_at"));
        t.setServiceName(rs.getString("service_name"));
        t.setServiceCode(rs.getString("service_code"));
        t.setAvgServiceMinutes(rs.getInt("avg_service_minutes"));
        t.setCustomerName(rs.getString("customer_name"));
        t.setCustomerPhone(rs.getString("customer_phone"));
        t.setCounterName(rs.getString("counter_name"));
        return t;
    }
}
