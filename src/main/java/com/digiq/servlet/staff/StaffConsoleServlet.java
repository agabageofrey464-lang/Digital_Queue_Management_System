package com.digiq.servlet.staff;

import com.digiq.dao.CounterDAO;
import com.digiq.dao.ServiceLogDAO;
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
import java.util.Collections;

/**
 * The counter console: who is at the desk now, who is next, and the scanner.
 *
 * <p>A staff member with no counter assigned still gets the page, with an explanation,
 * rather than an error - the admin simply has not linked them yet.</p>
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
            Counter counter = counterDAO.findByStaffId(user.getId());
            request.setAttribute("counter", counter);

            if (counter != null) {
                request.setAttribute("currentToken", tokenDAO.findActiveByCounter(counter.getId()));
                request.setAttribute("queue", tokenDAO.findPendingQueue(counter.getServiceId()));
                request.setAttribute("waitingCount", tokenDAO.countWaiting(counter.getServiceId()));
            } else {
                request.setAttribute("queue", Collections.emptyList());
                request.setAttribute("waitingCount", 0);
            }

            request.setAttribute("recentActivity", logDAO.findRecent(12));
            request.setAttribute("pageTitle", "Counter console");
            request.setAttribute("navActive", "console");
            render(request, response, "staff/console");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/");
        }
    }
}
