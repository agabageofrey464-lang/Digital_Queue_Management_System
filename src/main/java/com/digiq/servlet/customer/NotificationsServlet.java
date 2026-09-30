package com.digiq.servlet.customer;

import com.digiq.dao.NotificationDAO;
import com.digiq.model.Notification;
import com.digiq.model.User;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Json;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The customer's alert feed.
 *
 * <p>Serves the page for a normal request and JSON for the poll the bell icon makes
 * after a WebSocket event, so the same data has one source.</p>
 */
@WebServlet("/customer/notifications")
public class NotificationsServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Web.currentUser(request);
        try {
            List<Notification> items = notificationDAO.findByUser(user.getId(), 50);

            if (Web.wantsJson(request)) {
                List<Map<String, Object>> payload = new ArrayList<>();
                for (Notification n : items) {
                    payload.add(Json.map(
                            "id", n.getId(),
                            "title", n.getTitle(),
                            "message", n.getMessage(),
                            "type", n.getType(),
                            "read", n.isRead(),
                            "tokenNumber", n.getTokenNumber(),
                            "createdAt", n.getCreatedAt() == null ? null : n.getCreatedAt().toString()));
                }
                Json.write(response, Json.map(
                        "ok", true,
                        "unread", notificationDAO.countUnread(user.getId()),
                        "items", payload));
                return;
            }

            request.setAttribute("notifications", items);
            request.setAttribute("pageTitle", "Notifications");
            request.setAttribute("navActive", "alerts");
            render(request, response, "customer/notifications");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/home");
        }
    }

    /** Marks the whole feed as read. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = Web.currentUser(request);
        try {
            notificationDAO.markAllRead(user.getId());
            if (Web.wantsJson(request)) {
                Json.write(response, Json.map("ok", true, "unread", 0));
                return;
            }
            redirect(request, response, "/customer/notifications");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/notifications");
        }
    }
}
