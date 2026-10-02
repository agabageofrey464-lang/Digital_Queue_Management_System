package com.digiq.servlet.customer;

import com.digiq.model.User;
// Holds the rule about which tokens may still be cancelled.
import com.digiq.service.QueueService;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Lets a customer give up a place they no longer need.
 *
 * <p>Only a token still PENDING can be cancelled. Once a counter has called it the
 * customer is already being dealt with, so closing it out becomes the staff
 * member's job - otherwise a customer could cancel from their phone while standing
 * at the desk and leave the counter with a token it cannot finish.</p>
 *
 * <p>POST only: this changes state, so it must not be reachable by a link.</p>
 */
@WebServlet("/customer/cancel")
public class CancelTokenServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final QueueService queueService = new QueueService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        User user = Web.currentUser(request);
        int tokenId = Web.intParam(request, "tokenId", 0);

        try {
            // The signed-in id is passed separately so the service can confirm the
            // token actually belongs to the person asking. It returns false both for
            // "not yours" and for "too late", which keeps the two indistinguishable
            // from outside.
            if (queueService.cancelToken(tokenId, user.getId())) {
                Web.flashSuccess(request, "Your token has been cancelled.");
            } else {
                Web.flashError(request,
                        "That token can no longer be cancelled - it has already been called.");
            }

            // Back to the history page either way, where the new status is visible.
            redirect(request, response, "/customer/tokens");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/tokens");
        }
    }
}
