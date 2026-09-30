package com.digiq.service;

import com.digiq.dao.CounterDAO;
import com.digiq.dao.NotificationDAO;
import com.digiq.dao.TokenDAO;
import com.digiq.model.Counter;
import com.digiq.model.Token;
import com.digiq.model.TokenStatus;
import com.digiq.websocket.QueueBroadcaster;

import java.sql.SQLException;
import java.util.List;

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

    /** A customer's live tokens, each with its current queue position filled in. */
    public List<Token> activeTokensFor(int customerId) throws SQLException {
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
