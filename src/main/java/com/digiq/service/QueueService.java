package com.digiq.service;

import com.digiq.dao.CounterDAO;
import com.digiq.dao.NotificationDAO;
import com.digiq.dao.TokenDAO;
import com.digiq.model.Counter;
import com.digiq.model.Token;
import com.digiq.model.TokenStatus;
import com.digiq.websocket.QueueBroadcaster;

import java.sql.SQLException;
// Used to decide whether the daily sweep has already run today.
import java.time.LocalDate;
import java.util.List;
// Lets two concurrent requests race for the sweep without both performing it.
import java.util.concurrent.atomic.AtomicReference;

/**
 * The queue rules in one place.
 *
 * <p>Servlets call this rather than the DAOs directly, so that every state change goes
 * through the same three steps: persist it, raise the customer notification, then push
 * the event to the live screens. Keeping that order means a screen never learns about a
 * change the database has not committed.</p>
 */
public class QueueService {

    /** A customer this close to the front gets the "almost your turn" alert. */
    private static final int APPROACHING_THRESHOLD = 3;

    /**
     * The date the stale-token sweep last ran.
     *
     * <p>Static, so every request in this JVM shares one value, and an
     * {@link AtomicReference} so two simultaneous first-requests-of-the-day cannot
     * both decide to run it. A scheduled job would be tidier, but this needs no
     * extra thread and no container-specific configuration, and the work is
     * idempotent anyway - running it twice simply updates nothing the second time.</p>
     */
    private static final AtomicReference<LocalDate> LAST_SWEEP = new AtomicReference<>();

    private final TokenDAO tokenDAO = new TokenDAO();
    private final CounterDAO counterDAO = new CounterDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    // ------------------------------------------------------------------
    //  Customer actions
    // ------------------------------------------------------------------

    public Token bookToken(int serviceId, int customerId, boolean priority) throws SQLException {
        Token token = tokenDAO.issue(serviceId, customerId, priority);
        if (token == null) {
            return null;
        }
        token.setPositionInQueue(tokenDAO.positionOf(token));

        notificationDAO.insert(customerId, token.getId(), "Token booked",
                "Your token " + token.getTokenNumber() + " for " + token.getServiceName()
                        + " is confirmed. You are number " + token.getPositionInQueue() + " in the queue.",
                "INFO");

        QueueBroadcaster.tokenIssued(token, tokenDAO.countWaiting(serviceId));
        return token;
    }

    public boolean cancelToken(int tokenId, int customerId) throws SQLException {
        Token token = tokenDAO.findById(tokenId);
        if (token == null || token.getCustomerId() != customerId) {
            return false;
        }
        if (token.getStatus() != TokenStatus.PENDING) {
            // Once a counter has called it, only staff may close it out.
            return false;
        }
        Token updated = tokenDAO.changeStatus(tokenId, TokenStatus.CANCELLED, null, null,
                "Cancelled by the customer");
        if (updated == null) {
            return false;
        }
        QueueBroadcaster.tokenUpdated(updated);
        refreshApproachingAlerts(updated.getServiceId());
        return true;
    }

    // ------------------------------------------------------------------
    //  Counter staff actions
    // ------------------------------------------------------------------

    /**
     * Pulls the next waiting customer to this counter.
     *
     * @return the called token, or null when nobody is waiting
     */
    public Token callNext(Counter counter, int staffId) throws SQLException {
        if (!counter.getStatus().canServe()) {
            return null;
        }

        // Clear out anything left over from a previous day before pulling. The queue
        // query already filters on today, so this does not change who is called - it
        // keeps the counts and the customer's view honest.
        sweepStaleTokens();
        Token token = tokenDAO.callNext(counter.getId(), counter.getServiceId(), staffId);
        if (token == null) {
            return null;
        }

        notificationDAO.insert(token.getCustomerId(), token.getId(), "It is your turn",
                "Please proceed to " + counter.getName() + " for " + token.getServiceName() + ".",
                "CALLED");

        QueueBroadcaster.tokenCalled(token, counter);
        refreshApproachingAlerts(counter.getServiceId());
        return token;
    }

    public Token complete(int tokenId, Counter counter, int staffId, String note) throws SQLException {
        Token token = tokenDAO.changeStatus(tokenId, TokenStatus.COMPLETED,
                counter == null ? null : counter.getId(), staffId, note);
        if (token == null) {
            return null;
        }
        notificationDAO.insert(token.getCustomerId(), token.getId(), "Service complete",
                "Token " + token.getTokenNumber() + " has been served. Thank you for visiting.",
                "COMPLETED");
        QueueBroadcaster.tokenUpdated(token);
        return token;
    }

    public Token markNoShow(int tokenId, Counter counter, int staffId) throws SQLException {
        Token token = tokenDAO.changeStatus(tokenId, TokenStatus.NO_SHOW,
                counter == null ? null : counter.getId(), staffId, "Customer did not present");
        if (token == null) {
            return null;
        }
        notificationDAO.insert(token.getCustomerId(), token.getId(), "Token closed",
                "Token " + token.getTokenNumber() + " was marked as a no-show. Please book again if still needed.",
                "INFO");
        QueueBroadcaster.tokenUpdated(token);
        return token;
    }

    public void setCounterStatus(Counter counter, com.digiq.model.CounterStatus status) throws SQLException {
        counterDAO.updateStatus(counter.getId(), status);
        counter.setStatus(status);
        QueueBroadcaster.counterUpdated(counter);
    }

    // ------------------------------------------------------------------
    //  Notifications
    // ------------------------------------------------------------------

    /**
     * Raises the "you are nearly up" alert for whoever is now near the front.
     *
     * <p>{@link NotificationDAO#exists} guards it, so a customer is told once per token
     * rather than every time somebody ahead of them is served.</p>
     */
    public void refreshApproachingAlerts(int serviceId) throws SQLException {
        List<Token> queue = tokenDAO.findPendingQueue(serviceId);
        int position = 0;
        for (Token token : queue) {
            position++;
            if (position > APPROACHING_THRESHOLD) {
                break;
            }
            if (notificationDAO.exists(token.getCustomerId(), token.getId(), "APPROACHING")) {
                continue;
            }
            notificationDAO.insert(token.getCustomerId(), token.getId(), "Your turn is approaching",
                    "Token " + token.getTokenNumber() + " is number " + position
                            + " in the queue. Please stay close to the service area.",
                    "APPROACHING");
        }
        QueueBroadcaster.queueChanged(serviceId);
    }

    // ------------------------------------------------------------------
    //  Reads used by several screens
    // ------------------------------------------------------------------

    /**
     * Expires yesterday's unserved tokens, at most once per calendar day.
     *
     * <p>Called from the paths that would otherwise display a stale token as though
     * it were still waiting, and once at startup. Cheap to call repeatedly: after
     * the first run of the day it does nothing but compare two dates.</p>
     *
     * @return the number expired, or 0 when the sweep had already run today
     */
    public int sweepStaleTokens() throws SQLException {
        // Server-local date. The sweep compares against CURDATE() in SQL, so a
        // mismatched JVM timezone would at worst delay it by one request.
        LocalDate today = LocalDate.now();

        // What the last run recorded. Null on the very first call after a restart.
        LocalDate previous = LAST_SWEEP.get();

        // Already done today - the overwhelmingly common case.
        if (today.equals(previous)) {
            return 0;
        }

        // Claim the day atomically. If another thread got here first, its
        // compareAndSet succeeded and ours fails, so only one of us does the work.
        if (!LAST_SWEEP.compareAndSet(previous, today)) {
            return 0;
        }

        // Safe to run even if it turns out there is nothing to expire.
        return tokenDAO.expireStale();
    }

    /** A customer's live tokens, each with its current queue position filled in. */
    public List<Token> activeTokensFor(int customerId) throws SQLException {
        // Before reporting what is live, make sure nothing from a previous day is
        // still masquerading as waiting.
        sweepStaleTokens();

        List<Token> tokens = tokenDAO.findActiveByCustomer(customerId);
        for (Token token : tokens) {
            token.setPositionInQueue(tokenDAO.positionOf(token));
        }
        return tokens;
    }

    public Token withPosition(Token token) throws SQLException {
        if (token != null) {
            token.setPositionInQueue(tokenDAO.positionOf(token));
        }
        return token;
    }
}
