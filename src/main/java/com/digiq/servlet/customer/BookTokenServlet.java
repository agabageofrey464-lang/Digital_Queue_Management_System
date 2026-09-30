package com.digiq.servlet.customer;

import com.digiq.model.Token;
import com.digiq.model.User;
import com.digiq.service.QueueService;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Issues a token for the chosen service. */
@WebServlet("/customer/book")
public class BookTokenServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final QueueService queueService = new QueueService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = Web.currentUser(request);
        int serviceId = Web.intParam(request, "serviceId", 0);
        boolean priority = Web.boolParam(request, "priority");

        if (serviceId <= 0) {
            Web.flashError(request, "Choose a service before booking.");
            redirect(request, response, "/customer/home");
            return;
        }

        try {
            Token token = queueService.bookToken(serviceId, user.getId(), priority);
            if (token == null) {
                Web.flashError(request, "That service is not available right now.");
                redirect(request, response, "/customer/home");
                return;
            }
            Web.flashSuccess(request, "Token " + token.getTokenNumber() + " issued.");
            redirect(request, response, "/customer/token?id=" + token.getId());
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/home");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        redirect(request, response, "/customer/home");
    }
}
