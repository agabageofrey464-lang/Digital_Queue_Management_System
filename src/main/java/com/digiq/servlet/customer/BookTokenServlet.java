package com.digiq.servlet.customer;

// The token that comes back once one has been issued.
import com.digiq.model.Token;
// The signed-in account, read from the session.
import com.digiq.model.User;
// Owns the booking rule: persist, notify, then broadcast.
import com.digiq.service.QueueService;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Issues a token for the service the customer picked.
 *
 * <p>Accepts only POST. Booking changes state, and a GET would let a token be
 * issued by a prefetch, a refresh or a pasted link - so the GET handler
 * deliberately does nothing but redirect.</p>
 */
// Behind /customer/*, so AuthFilter has already guaranteed a signed-in CUSTOMER.
@WebServlet("/customer/book")
public class BookTokenServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final QueueService queueService = new QueueService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Safe to use without a null check: AuthFilter rejects the request before
        // it reaches this servlet if nobody is signed in.
        User user = Web.currentUser(request);

        // Falls back to 0 when the field is missing or not a number, which the
        // guard below then rejects.
        int serviceId = Web.intParam(request, "serviceId", 0);

        // The checkbox is only submitted when ticked, so absence means "normal".
        boolean priority = Web.boolParam(request, "priority");

        // Nothing selected, or the value was tampered with.
        if (serviceId <= 0) {
            Web.flashError(request, "Choose a service before booking.");
            redirect(request, response, "/customer/home");
            return;
        }

        try {
            // Takes the customer id from the session, never from the form - otherwise
            // anyone could book a token in somebody else's name by editing the POST.
            Token token = queueService.bookToken(serviceId, user.getId(), priority);

            // Null means the service id did not match an active service.
            if (token == null) {
                Web.flashError(request, "That service is not available right now.");
                redirect(request, response, "/customer/home");
                return;
            }

            Web.flashSuccess(request, "Token " + token.getTokenNumber() + " issued.");

            // Redirect rather than render, so a refresh on the ticket page cannot
            // re-submit the booking and issue a second token (Post/Redirect/Get).
            redirect(request, response, "/customer/token?id=" + token.getId());

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/home");
        }
    }

    /** A GET here is never meaningful, so send the visitor somewhere useful. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        redirect(request, response, "/customer/home");
    }
}
