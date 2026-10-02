package com.digiq.servlet.customer;

// Unread alert count for the sidebar badge.
import com.digiq.dao.NotificationDAO;
// Bookable services, each with its current queue depth.
import com.digiq.dao.ServiceDAO;
import com.digiq.dao.TokenDAO;
import com.digiq.model.User;
import com.digiq.service.QueueService;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * The customer's home page: anything live at the top, services to book underneath.
 *
 * <p>Four separate reads rather than one joined query. They answer genuinely
 * different questions, each is indexed, and keeping them apart means a change to
 * one section cannot quietly break another.</p>
 */
@WebServlet("/customer/home")
public class CustomerHomeServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final TokenDAO tokenDAO = new TokenDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final QueueService queueService = new QueueService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = Web.currentUser(request);

        try {
            // Active services with today's waiting count and how many counters are
            // open, which is what the estimated wait on each card is derived from.
            request.setAttribute("services", serviceDAO.findActiveWithQueueStats());

            // Today's live tokens, each with its queue position already worked out.
            request.setAttribute("activeTokens", queueService.activeTokensFor(user.getId()));

            // A short history strip under the service cards. Five is enough to be
            // useful without turning the home page into the history page.
            request.setAttribute("recentTokens", tokenDAO.findByCustomer(user.getId(), 5));

            // Drives the number on the Notifications link in the sidebar.
            request.setAttribute("unreadCount", notificationDAO.countUnread(user.getId()));

            request.setAttribute("pageTitle", "Book a token");
            request.setAttribute("navActive", "home");

            render(request, response, "customer/home");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/");
        }
    }
}
