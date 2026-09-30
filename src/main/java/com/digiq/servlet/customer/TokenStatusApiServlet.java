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
 * <p>The tracking page calls this whenever the WebSocket reports a change on this
 * customer's service, rather than trusting the pushed payload: the socket says
 * <em>something moved</em>, the database says <em>where you now stand</em>.</p>
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
            if (token == null || token.getCustomerId() != user.getId()) {
                Json.error(response, 404, "Token not found.");
                return;
            }
            queueService.withPosition(token);

            Json.write(response, Json.map(
                    "ok", true,
                    "tokenNumber", token.getTokenNumber(),
                    "status", token.getStatus().name(),
                    "statusLabel", token.getStatus().getLabel(),
                    "tone", token.getStatus().getTone(),
                    "position", token.getPositionInQueue(),
                    "estimatedWait", token.getEstimatedWaitMinutes(),
                    "counterName", token.getCounterName(),
                    "serviceName", token.getServiceName(),
                    "serviceId", token.getServiceId(),
                    "unread", notificationDAO.countUnread(user.getId())));
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/home");
        }
    }
}
