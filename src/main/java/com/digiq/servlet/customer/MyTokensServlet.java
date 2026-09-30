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

/** Full booking history for the signed-in customer. */
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
            request.setAttribute("tokens", tokenDAO.findByCustomer(user.getId(), 100));
            request.setAttribute("activeTokens", queueService.activeTokensFor(user.getId()));
            request.setAttribute("pageTitle", "My tokens");
            request.setAttribute("navActive", "tokens");
            render(request, response, "customer/tokens");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/home");
        }
    }
}
