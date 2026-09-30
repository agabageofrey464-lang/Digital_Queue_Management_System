package com.digiq.websocket;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * The single WebSocket every live screen connects to.
 *
 * <p>The display board, the staff console, the customer tracking page and the admin
 * dashboard all subscribe here. The server pushes one event stream and each client
 * keeps what it cares about, which keeps the protocol small enough to reason about.</p>
 *
 * <p>Endpoint: {@code ws://<host>/digiq/ws/queue}</p>
 */
@ServerEndpoint("/ws/queue")
public class QueueEndpoint {

    @OnOpen
    public void onOpen(Session session) {
        // A slow display panel must never stall the servlet thread that broadcasts.
        session.setMaxIdleTimeout(0);
        QueueBroadcaster.register(session);
        QueueBroadcaster.sendTo(session, QueueBroadcaster.hello());
    }

    /**
     * The client only ever sends {@code "ping"}; answering it keeps proxies from
     * dropping an idle display board overnight.
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        if ("ping".equals(message)) {
            QueueBroadcaster.sendTo(session, QueueBroadcaster.pong());
        }
    }

    @OnClose
    public void onClose(Session session) {
        QueueBroadcaster.unregister(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        QueueBroadcaster.unregister(session);
    }
}
