package com.digiq.servlet.staff;

import com.digiq.dao.CounterDAO;
import com.digiq.dao.TokenDAO;
import com.digiq.model.Counter;
import com.digiq.model.CounterStatus;
import com.digiq.model.Token;
import com.digiq.model.User;
// Persists, notifies and broadcasts - in that order.
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
 * <p>One endpoint with an {@code action} parameter rather than six separate URLs.
 * All six need the same two things first - find this staff member's counter, and
 * confirm they are allowed to touch it - so a single entry point means that check
 * cannot be forgotten when an action is added.</p>
 *
 * <p>Answers JSON when the console calls it with fetch, which is the normal path and
 * leaves the page in place. Falls back to a redirect with a flash message when
 * JavaScript is unavailable, so the console still works without it.</p>
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

        // Empty string rather than null, so the switch below always has something
        // to match and falls through to the "unknown action" branch.
        String action = Web.param(request, "action", "");

        try {
            // Resolved from the session, never from the request. This is the single
            // check that stops one staff member driving another counter.
            Counter counter = counterDAO.findByStaffId(user.getId());

            if (counter == null) {
                respond(request, response, false, "No counter is assigned to your account.", null);
                return;
            }

            switch (action) {
                case "call":
                    // Pull the next waiting customer.
                    handleCall(request, response, counter, user);
                    break;
                case "complete":
                    // Served successfully - the true flag selects COMPLETED.
                    handleTerminal(request, response, counter, user, true);
                    break;
                case "noshow":
                    // Called but nobody came - false selects NO_SHOW.
                    handleTerminal(request, response, counter, user, false);
                    break;
                case "open":
                    // setCounterStatus also broadcasts, so the public board reacts.
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
                    // A typo in the client, or a hand-crafted request.
                    respond(request, response, false, "Unknown action.", null);
            }

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/staff/console");
        }
    }

    /**
     * Calls the next waiting customer to this counter.
     *
     * <p>Two guards run before the queue is touched, and both exist because of how
     * a counter is actually used rather than for technical reasons.</p>
     */
    private void handleCall(HttpServletRequest request, HttpServletResponse response,
                            Counter counter, User user) throws SQLException, IOException {

        // A PAUSED or CLOSED counter must not take customers. Checked here rather
        // than only in the UI, since the button can be bypassed.
        if (!counter.getStatus().canServe()) {
            respond(request, response, false,
                    "Open the counter before calling the next customer.", null);
            return;
        }

        // Refuse to call a second customer while one is still at the desk. Without
        // this, a double-click would strand the first token IN_SERVICE forever and
        // the board would show the wrong number.
        Token current = tokenDAO.findActiveByCounter(counter.getId());
        if (current != null) {
            respond(request, response, false,
                    "Finish " + current.getTokenNumber() + " before calling the next customer.", null);
            return;
        }

        // Null means the queue is empty. Note this is not an error - it is the
        // normal answer during a quiet period.
        Token token = queueService.callNext(counter, user.getId());
        if (token == null) {
            respond(request, response, false, "Nobody is waiting for this service.", null);
            return;
        }

        respond(request, response, true, "Now serving " + token.getTokenNumber() + ".", token);
    }

    /**
     * Closes a token out, either as COMPLETED or as NO_SHOW.
     *
     * @param completed true for a served customer, false for one who never appeared
     */
    private void handleTerminal(HttpServletRequest request, HttpServletResponse response,
                                Counter counter, User user, boolean completed)
            throws SQLException, IOException {

        int tokenId = Web.intParam(request, "tokenId", 0);

        // The console normally names the token explicitly, but falling back to
        // whatever is at this counter means the keyboard path still works.
        Token target = tokenId > 0 ? tokenDAO.findById(tokenId) : tokenDAO.findActiveByCounter(counter.getId());

        if (target == null) {
            respond(request, response, false, "There is no active token at this counter.", null);
            return;
        }

        // The id came from the client, so confirm the token really is at this
        // counter. Otherwise a staff member could complete a colleague's customer.
        if (target.getCounterId() == null || target.getCounterId() != counter.getId()) {
            respond(request, response, false, "That token belongs to another counter.", null);
            return;
        }

        // Both paths write an audit row and notify the customer.
        Token updated = completed
                ? queueService.complete(target.getId(), counter, user.getId(), Web.param(request, "note"))
                : queueService.markNoShow(target.getId(), counter, user.getId());

        // Closing a token shifts everyone behind it forward, so whoever is now near
        // the front gets their "almost your turn" alert.
        queueService.refreshApproachingAlerts(counter.getServiceId());

        respond(request, response, true,
                (completed ? "Completed " : "Marked as no-show: ") + target.getTokenNumber() + ".", updated);
    }

    /**
     * Replies in whichever form the caller expects.
     *
     * <p>Having one place that decides between JSON and a redirect keeps every
     * branch above free of that concern.</p>
     */
    private void respond(HttpServletRequest request, HttpServletResponse response,
                         boolean ok, String message, Token token) throws IOException {

        // True when the console called this with fetch.
        if (Web.wantsJson(request)) {
            Json.write(response, Json.map(
                    "ok", ok,
                    "message", message,
                    // Null-safe: several branches have no token to report.
                    "tokenNumber", token == null ? null : token.getTokenNumber(),
                    "customerName", token == null ? null : token.getCustomerName(),
                    "status", token == null ? null : token.getStatus().name()));
            return;
        }

        // No-JavaScript path: stash the message and reload the console.
        if (ok) {
            Web.flashSuccess(request, message);
        } else {
            Web.flashError(request, message);
        }
        redirect(request, response, "/staff/console");
    }
}
