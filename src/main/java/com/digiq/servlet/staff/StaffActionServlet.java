package com.digiq.servlet.staff;

import com.digiq.dao.CounterDAO;
import com.digiq.dao.TokenDAO;
import com.digiq.model.Counter;
import com.digiq.model.CounterStatus;
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
 * Every button on the counter console posts here.
 *
 * <p>Answers JSON when the console calls it with fetch (the normal path, which leaves
 * the page in place) and falls back to a redirect when JavaScript is unavailable.</p>
 */
@WebServlet("/staff/action")
public class StaffActionServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final CounterDAO counterDAO = new CounterDAO();
    private final TokenDAO tokenDAO = new TokenDAO();
    private final QueueService queueService = new QueueService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = Web.currentUser(request);
        String action = Web.param(request, "action", "");

        try {
            Counter counter = counterDAO.findByStaffId(user.getId());
            if (counter == null) {
                respond(request, response, false, "No counter is assigned to your account.", null);
                return;
            }

            switch (action) {
                case "call":
                    handleCall(request, response, counter, user);
                    break;
                case "complete":
                    handleTerminal(request, response, counter, user, true);
                    break;
                case "noshow":
                    handleTerminal(request, response, counter, user, false);
                    break;
                case "open":
                    queueService.setCounterStatus(counter, CounterStatus.OPEN);
                    respond(request, response, true, counter.getName() + " is now open.", null);
                    break;
                case "pause":
                    queueService.setCounterStatus(counter, CounterStatus.PAUSED);
                    respond(request, response, true, counter.getName() + " is paused.", null);
                    break;
                case "close":
                    queueService.setCounterStatus(counter, CounterStatus.CLOSED);
                    respond(request, response, true, counter.getName() + " is closed.", null);
                    break;
                default:
                    respond(request, response, false, "Unknown action.", null);
            }
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/staff/console");
        }
    }

    private void handleCall(HttpServletRequest request, HttpServletResponse response,
                            Counter counter, User user) throws SQLException, IOException {
        if (!counter.getStatus().canServe()) {
            respond(request, response, false,
                    "Open the counter before calling the next customer.", null);
            return;
        }
        Token current = tokenDAO.findActiveByCounter(counter.getId());
        if (current != null) {
            respond(request, response, false,
                    "Finish " + current.getTokenNumber() + " before calling the next customer.", null);
            return;
        }
        Token token = queueService.callNext(counter, user.getId());
        if (token == null) {
            respond(request, response, false, "Nobody is waiting for this service.", null);
            return;
        }
        respond(request, response, true, "Now serving " + token.getTokenNumber() + ".", token);
    }

    private void handleTerminal(HttpServletRequest request, HttpServletResponse response,
                                Counter counter, User user, boolean completed)
            throws SQLException, IOException {
        int tokenId = Web.intParam(request, "tokenId", 0);
        Token target = tokenId > 0 ? tokenDAO.findById(tokenId) : tokenDAO.findActiveByCounter(counter.getId());

        if (target == null) {
            respond(request, response, false, "There is no active token at this counter.", null);
            return;
        }
        if (target.getCounterId() == null || target.getCounterId() != counter.getId()) {
            respond(request, response, false, "That token belongs to another counter.", null);
            return;
        }

        Token updated = completed
                ? queueService.complete(target.getId(), counter, user.getId(), Web.param(request, "note"))
                : queueService.markNoShow(target.getId(), counter, user.getId());

        queueService.refreshApproachingAlerts(counter.getServiceId());
        respond(request, response, true,
                (completed ? "Completed " : "Marked as no-show: ") + target.getTokenNumber() + ".", updated);
    }

    private void respond(HttpServletRequest request, HttpServletResponse response,
                         boolean ok, String message, Token token) throws IOException {
        if (Web.wantsJson(request)) {
            Json.write(response, Json.map(
                    "ok", ok,
                    "message", message,
                    "tokenNumber", token == null ? null : token.getTokenNumber(),
                    "customerName", token == null ? null : token.getCustomerName(),
                    "status", token == null ? null : token.getStatus().name()));
            return;
        }
        if (ok) {
            Web.flashSuccess(request, message);
        } else {
            Web.flashError(request, message);
        }
        redirect(request, response, "/staff/console");
    }
}
