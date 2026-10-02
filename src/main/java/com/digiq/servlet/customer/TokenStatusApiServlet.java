package com.digiq.servlet.customer;

import com.digiq.dao.NotificationDAO;
import com.digiq.dao.TokenDAO;
import com.digiq.model.Token;
import com.digiq.model.User;
import com.digiq.service.QueueService;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Json;
import com.digiq.util.Web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Current position and status of one token, as JSON.
 *
 * <p>This is the other half of the real-time design. The WebSocket announces only
 * that <em>something moved</em> on a service; the customer's page then calls this
 * to find out where they now stand.</p>
 *
 * <p>The position is deliberately never pushed over the socket. It is different for
 * every customer, so broadcasting it would mean either sending one tailored message
 * per connection or letting clients work it out from data they should not have. A
 * short read against the database is simpler and cannot drift.</p>
 */
@WebServlet("/customer/status")
public class TokenStatusApiServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TokenDAO tokenDAO = new TokenDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final QueueService queueService = new QueueService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        User user = Web.currentUser(request);
        int id = Web.intParam(request, "id", 0);

        try {
            Token token = tokenDAO.findById(id);

            // 404 for both "no such token" and "not yours". This endpoint is polled
            // frequently, so it must not become a way to enumerate other people's
            // tickets by trying ids.
            if (token == null || token.getCustomerId() != user.getId()) {
                Json.error(response, 404, "Token not found.");
                return;
            }

            // Recalculated per call - that is the whole point of this endpoint.
            queueService.withPosition(token);

            Json.write(response, Json.map(
                    "ok", true,
                    "tokenNumber", token.getTokenNumber(),
                    "status", token.getStatus().name(),
                    // Label and tone come from the enum, so the colours on screen stay
                    // in step with the server rather than being re-derived in JavaScript.
                    "statusLabel", token.getStatus().getLabel(),
                    "tone", token.getStatus().getTone(),
                    // 0 once the token is no longer waiting.
                    "position", token.getPositionInQueue(),
                    "estimatedWait", token.getEstimatedWaitMinutes(),
                    // Null until a counter has called them.
                    "counterName", token.getCounterName(),
                    "serviceName", token.getServiceName(),
                    // Lets the page decide whether a broadcast concerns it.
                    "serviceId", token.getServiceId(),
                    // Piggy-backed so the sidebar badge updates without a second request.
                    "unread", notificationDAO.countUnread(user.getId())));

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/home");
        }
    }
}
