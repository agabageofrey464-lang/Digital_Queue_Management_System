package com.digiq.servlet.customer;

// Reads the audit trail rows shown as the progress timeline.
import com.digiq.dao.ServiceLogDAO;
// Loads the token itself.
import com.digiq.dao.TokenDAO;
import com.digiq.model.Token;
import com.digiq.model.User;
// Used to work out how many people are still ahead.
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
 * The live ticket: token number, QR code, queue position and progress so far.
 *
 * <p>Renders the state once on the server. After that the page keeps itself up to
 * date over the WebSocket, re-reading {@code /customer/status} whenever the queue
 * moves, so this servlet is only ever hit on a real page load.</p>
 */
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

        // Which ticket was asked for. Anyone can edit this number in the URL,
        // which is exactly why the ownership check below exists.
        int id = Web.intParam(request, "id", 0);

        try {
            Token token = tokenDAO.findById(id);

            // Two failures handled as one. Saying "not found" for a token that
            // exists but belongs to someone else stops the URL being used to probe
            // for valid ids, and the user gets the same message either way.
            if (token == null || token.getCustomerId() != user.getId()) {
                Web.flashError(request, "That token could not be found.");
                redirect(request, response, "/customer/tokens");
                return;
            }

            // Fills in positionInQueue, which drives the big number on the page.
            // Returns 0 for anything no longer waiting.
            queueService.withPosition(token);

            // Read by the JSP as ${token}.
            request.setAttribute("token", token);

            // Every status change recorded for this token, oldest first.
            request.setAttribute("timeline", logDAO.findByToken(token.getId()));

            request.setAttribute("pageTitle", "Token " + token.getTokenNumber());

            // Highlights "My tokens" in the sidebar.
            request.setAttribute("navActive", "tokens");

            render(request, response, "customer/token");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/home");
        }
    }
}
