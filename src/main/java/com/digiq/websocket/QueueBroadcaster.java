package com.digiq.websocket;

import com.digiq.model.Counter;
import com.digiq.model.Token;
import com.digiq.util.Json;

import javax.websocket.Session;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Fans queue events out to every connected screen.
 *
 * <p>Sessions live in a {@link CopyOnWriteArraySet} because the set is read on every
 * broadcast and written only when a screen connects or disconnects. Each write is
 * synchronised on its own session: the WebSocket spec forbids two concurrent sends on
 * one connection, and without the lock a staff member calling the next token while the
 * board refreshes can tear a frame.</p>
 */
public final class QueueBroadcaster {

    public static final String TOKEN_ISSUED = "TOKEN_ISSUED";
    public static final String TOKEN_CALLED = "TOKEN_CALLED";
    public static final String TOKEN_UPDATED = "TOKEN_UPDATED";
    public static final String COUNTER_UPDATED = "COUNTER_UPDATED";
    public static final String QUEUE_CHANGED = "QUEUE_CHANGED";

    private static final Set<Session> SESSIONS = new CopyOnWriteArraySet<>();

    private QueueBroadcaster() {
    }

    static void register(Session session) {
        SESSIONS.add(session);
    }

    static void unregister(Session session) {
        SESSIONS.remove(session);
    }

    /**
     * The greeting sent to a screen as soon as it connects.
     *
     * <p>Wrapped in the same {@code {type, payload}} envelope as every other event -
     * a client handler must not have to special-case the first message it receives.</p>
     */
    static String hello() {
        return Json.stringify(Json.map(
                "type", "CONNECTED",
                "payload", Json.map("clients", SESSIONS.size())));
    }

    /** Reply to the client keepalive, in the same envelope. */
    static String pong() {
        return Json.stringify(Json.map("type", "PONG", "payload", Json.map()));
    }

    public static int connectedClients() {
        return SESSIONS.size();
    }

    // ------------------------------------------------------------------
    //  Event helpers - the servlets call these, never send() directly
    // ------------------------------------------------------------------

    public static void tokenIssued(Token token, int waitingCount) {
        broadcast(TOKEN_ISSUED, Json.map(
                "token", summarise(token),
                "serviceId", token.getServiceId(),
                "waiting", waitingCount));
    }

    public static void tokenCalled(Token token, Counter counter) {
        broadcast(TOKEN_CALLED, Json.map(
                "token", summarise(token),
                "counterId", counter.getId(),
                "counterName", counter.getName(),
                "serviceId", token.getServiceId()));
    }

    public static void tokenUpdated(Token token) {
        broadcast(TOKEN_UPDATED, Json.map(
                "token", summarise(token),
                "serviceId", token.getServiceId()));
    }

    public static void counterUpdated(Counter counter) {
        broadcast(COUNTER_UPDATED, Json.map(
                "counterId", counter.getId(),
                "counterName", counter.getName(),
                "status", counter.getStatus().name(),
                "serviceId", counter.getServiceId()));
    }

    /** Generic nudge telling every screen to re-pull its own data. */
    public static void queueChanged(int serviceId) {
        broadcast(QUEUE_CHANGED, Json.map("serviceId", serviceId));
    }

    // ------------------------------------------------------------------

    private static Map<String, Object> summarise(Token token) {
        return Json.map(
                "id", token.getId(),
                "tokenNumber", token.getTokenNumber(),
                "status", token.getStatus().name(),
                "statusLabel", token.getStatus().getLabel(),
                "serviceName", token.getServiceName(),
                "serviceCode", token.getServiceCode(),
                "counterName", token.getCounterName(),
                "customerId", token.getCustomerId(),
                "customerName", token.getCustomerName(),
                "priority", token.isPriority());
    }

    private static void broadcast(String type, Map<String, Object> payload) {
        String message = Json.stringify(Json.map("type", type, "payload", payload));
        for (Session session : SESSIONS) {
            sendTo(session, message);
        }
    }

    static void sendTo(Session session, String message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException | IllegalStateException ex) {
            // The screen went away mid-send; drop it rather than retrying.
            SESSIONS.remove(session);
        }
    }
}
