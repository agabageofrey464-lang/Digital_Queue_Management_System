package com.digiq.servlet.customer;

import com.digiq.dao.NotificationDAO;
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

/** The customer's home: live tokens on top, bookable services underneath. */
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
            request.setAttribute("services", serviceDAO.findActiveWithQueueStats());
            request.setAttribute("activeTokens", queueService.activeTokensFor(user.getId()));
            request.setAttribute("recentTokens", tokenDAO.findByCustomer(user.getId(), 5));
            request.setAttribute("unreadCount", notificationDAO.countUnread(user.getId()));
            request.setAttribute("pageTitle", "Book a token");
            request.setAttribute("navActive", "home");
            render(request, response, "customer/home");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/");
        }
    }
}
