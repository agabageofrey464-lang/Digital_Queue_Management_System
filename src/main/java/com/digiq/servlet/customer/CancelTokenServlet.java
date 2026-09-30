package com.digiq.servlet.customer;

import com.digiq.model.User;
import com.digiq.service.QueueService;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Lets a customer give up a place that has not been called yet. */
@WebServlet("/customer/cancel")
public class CancelTokenServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final QueueService queueService = new QueueService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = Web.currentUser(request);
        int tokenId = Web.intParam(request, "tokenId", 0);

        try {
            if (queueService.cancelToken(tokenId, user.getId())) {
                Web.flashSuccess(request, "Your token has been cancelled.");
            } else {
                Web.flashError(request,
                        "That token can no longer be cancelled - it has already been called.");
            }
            redirect(request, response, "/customer/tokens");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/tokens");
        }
    }
}
