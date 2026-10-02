package com.digiq.servlet.customer;

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
 * The customer's own booking history.
 *
 * <p>Two lists on one page: the tokens still live today, pulled out into cards at
 * the top with their queue position, and everything ever booked underneath. They
 * are fetched separately because the live ones need a position calculated per row
 * and the historical ones do not.</p>
 */
@WebServlet("/customer/tokens")
public class MyTokensServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TokenDAO tokenDAO = new TokenDAO();
    private final QueueService queueService = new QueueService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = Web.currentUser(request);

        try {
            // Capped at 100. Without a limit this query grows without bound for a
            // regular customer, and nobody scrolls past the first screen anyway.
            // The customer id comes from the session, so the query can only ever
            // return rows belonging to the person asking.
            request.setAttribute("tokens", tokenDAO.findByCustomer(user.getId(), 100));

            // Today's PENDING and IN_SERVICE tokens, each with its position filled in.
            request.setAttribute("activeTokens", queueService.activeTokensFor(user.getId()));

            request.setAttribute("pageTitle", "My tokens");
            request.setAttribute("navActive", "tokens");

            render(request, response, "customer/tokens");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/home");
        }
    }
}
