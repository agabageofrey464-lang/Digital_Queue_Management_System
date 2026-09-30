package com.digiq.servlet.customer;

import com.digiq.dao.ServiceLogDAO;
import com.digiq.dao.TokenDAO;
import com.digiq.model.Token;
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

/** The live ticket: token number, QR code, queue position and progress. */
@WebServlet("/customer/token")
public class TokenViewServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TokenDAO tokenDAO = new TokenDAO();
    private final ServiceLogDAO logDAO = new ServiceLogDAO();
    private final QueueService queueService = new QueueService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Web.currentUser(request);
        int id = Web.intParam(request, "id", 0);

        try {
            Token token = tokenDAO.findById(id);
            if (token == null || token.getCustomerId() != user.getId()) {
                Web.flashError(request, "That token could not be found.");
                redirect(request, response, "/customer/tokens");
                return;
            }
            queueService.withPosition(token);
            request.setAttribute("token", token);
            request.setAttribute("timeline", logDAO.findByToken(token.getId()));
            request.setAttribute("pageTitle", "Token " + token.getTokenNumber());
            request.setAttribute("navActive", "tokens");
            render(request, response, "customer/token");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/home");
        }
    }
}
