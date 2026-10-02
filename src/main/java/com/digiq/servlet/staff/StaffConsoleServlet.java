package com.digiq.servlet.staff;

// Finds which counter this staff member is signed in to.
import com.digiq.dao.CounterDAO;
// Recent activity feed.
import com.digiq.dao.ServiceLogDAO;
// The current token and the waiting queue.
import com.digiq.dao.TokenDAO;
import com.digiq.model.Counter;
import com.digiq.model.User;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
// Used so the view always receives a list, never null.
import java.util.Collections;

/**
 * The counter console: who is at the desk now, who is next, and the scanner.
 *
 * <p>Renders once per page load. Everything after that happens over fetch against
 * {@code /staff/action} and {@code /staff/scan}, so the staff member never loses
 * their place mid-transaction.</p>
 */
@WebServlet("/staff/console")
public class StaffConsoleServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final CounterDAO counterDAO = new CounterDAO();
    private final TokenDAO tokenDAO = new TokenDAO();
    private final ServiceLogDAO logDAO = new ServiceLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = Web.currentUser(request);

        try {
            // The counter is found from the signed-in staff id, never from a URL
            // parameter. That single decision is what stops one staff member from
            // operating another counter by editing a link.
            Counter counter = counterDAO.findByStaffId(user.getId());

            // May legitimately be null - the admin simply has not linked them yet.
            // The JSP renders an explanation in that case rather than an error.
            request.setAttribute("counter", counter);

            if (counter != null) {
                // Whatever this counter is serving right now, or null if idle.
                request.setAttribute("currentToken", tokenDAO.findActiveByCounter(counter.getId()));

                // Everyone still waiting for this counter's service, in serving order.
                request.setAttribute("queue", tokenDAO.findPendingQueue(counter.getServiceId()));

                // Shown as "N in line"; kept separate from the list because the
                // WebSocket updates this number without re-rendering the list.
                request.setAttribute("waitingCount", tokenDAO.countWaiting(counter.getServiceId()));
            } else {
                // Empty values rather than nulls, so the JSP needs no null checks.
                request.setAttribute("queue", Collections.emptyList());
                request.setAttribute("waitingCount", 0);
            }

            // Branch-wide, not just this counter - useful context for whoever is on shift.
            request.setAttribute("recentActivity", logDAO.findRecent(12));

            request.setAttribute("pageTitle", "Counter console");
            request.setAttribute("navActive", "console");

            render(request, response, "staff/console");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/");
        }
    }
}
