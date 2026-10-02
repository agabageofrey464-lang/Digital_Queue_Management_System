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
// Built by hand below so the JSON exposes only the fields the page needs.
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The customer's alert feed.
 *
 * <p>One URL serving two shapes: the full page on a normal visit, and JSON when the
 * bell icon re-checks after a queue event. Both read the same rows through the same
 * DAO call, so the page and the badge can never disagree about the unread count.</p>
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
            // Scoped to the signed-in user by the query itself, and capped at 50 -
            // nobody scrolls further, and the feed grows forever otherwise.
            List<Notification> items = notificationDAO.findByUser(user.getId(), 50);

            // The fetch path: return data, not markup.
            if (Web.wantsJson(request)) {

                // Deliberately not serialising the Notification beans directly. They
                // carry userId and tokenId, which the page has no use for, and mapping
                // explicitly means a field added to the bean cannot leak by accident.
                List<Map<String, Object>> payload = new ArrayList<>();

                for (Notification n : items) {
                    payload.add(Json.map(
                            "id", n.getId(),
                            "title", n.getTitle(),
                            "message", n.getMessage(),
                            // INFO / APPROACHING / CALLED / COMPLETED - picks the icon.
                            "type", n.getType(),
                            "read", n.isRead(),
                            // Null for alerts not tied to a token.
                            "tokenNumber", n.getTokenNumber(),
                            // Null-safe: toString() on a null Timestamp would throw.
                            "createdAt", n.getCreatedAt() == null ? null : n.getCreatedAt().toString()));
                }

                Json.write(response, Json.map(
                        "ok", true,
                        // Drives the sidebar badge.
                        "unread", notificationDAO.countUnread(user.getId()),
                        "items", payload));
                return;
            }

            // The ordinary page path.
            request.setAttribute("notifications", items);
            request.setAttribute("pageTitle", "Notifications");
            request.setAttribute("navActive", "alerts");

            render(request, response, "customer/notifications");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/home");
        }
    }

    /**
     * Marks the whole feed as read.
     *
     * <p>POST because it changes state. All-or-nothing rather than per-item: the
     * feed is short and read in one glance, so individual read flags would be
     * machinery with nothing to show for it.</p>
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        User user = Web.currentUser(request);

        try {
            // Scoped to this user inside the DAO.
            notificationDAO.markAllRead(user.getId());

            if (Web.wantsJson(request)) {
                // Returning the new count saves the client a follow-up request.
                Json.write(response, Json.map("ok", true, "unread", 0));
                return;
            }

            // Redirect rather than render, so a refresh does not re-post.
            redirect(request, response, "/customer/notifications");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/notifications");
        }
    }
}
