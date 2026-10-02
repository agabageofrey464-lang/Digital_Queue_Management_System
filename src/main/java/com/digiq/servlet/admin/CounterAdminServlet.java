package com.digiq.servlet.admin;

import com.digiq.dao.CounterDAO;
import com.digiq.dao.ServiceDAO;
import com.digiq.dao.UserDAO;
import com.digiq.model.Counter;
import com.digiq.model.CounterStatus;
import com.digiq.model.Role;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;
// So the public display board reacts to a counter being added or reassigned.
import com.digiq.websocket.QueueBroadcaster;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Counters: the service each one handles, and the staff member behind it.
 *
 * <p>Assigning a staff member here is what gives them a working console. Until an
 * admin links them to a counter, {@code /staff/console} has nothing to show, which
 * is why the console explains that rather than erroring.</p>
 */
@WebServlet("/admin/counters")
public class CounterAdminServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final CounterDAO counterDAO = new CounterDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            // Each row already carries its service name, staff name, current token and
            // today's completed count - all resolved in one query by the DAO.
            request.setAttribute("counters", counterDAO.findAll());

            // Populates the service dropdown in the create/edit dialog.
            request.setAttribute("services", serviceDAO.findAll());

            // Only STAFF accounts can be put behind a counter, so customers and other
            // admins are filtered out before the list ever reaches the page.
            request.setAttribute("staff", userDAO.findByRole(Role.STAFF));

            request.setAttribute("pageTitle", "Counters");
            request.setAttribute("navActive", "counters");

            render(request, response, "admin/counters");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/admin/dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String action = Web.param(request, "action", "save");

        try {
            if ("delete".equals(action)) {
                counterDAO.delete(Web.intParam(request, "id", 0));
                Web.flashSuccess(request, "Counter removed.");
                redirect(request, response, "/admin/counters");
                return;
            }

            int id = Web.intParam(request, "id", 0);
            String name = Web.param(request, "name");
            int serviceId = Web.intParam(request, "serviceId", 0);

            // A counter with no service cannot pull from any queue, so both are required.
            if (name == null || serviceId <= 0) {
                Web.flashError(request, "A counter needs a name and a service.");
                redirect(request, response, "/admin/counters");
                return;
            }

            Counter counter = new Counter();
            counter.setId(id);
            counter.setName(name);
            counter.setServiceId(serviceId);

            // The dropdown submits 0 for "Unassigned". The column is nullable, so that
            // is translated to null rather than stored as a user id of zero.
            int staffId = Web.intParam(request, "staffId", 0);
            counter.setStaffId(staffId > 0 ? staffId : null);

            // Falls back to CLOSED if the value is unrecognised - a counter whose state
            // cannot be read must not start serving.
            counter.setStatus(CounterStatus.from(Web.param(request, "status", "CLOSED")));

            if (id > 0) {
                counterDAO.update(counter);
                Web.flashSuccess(request, "Counter updated.");
            } else {
                // insert() writes the generated key back onto the object, so the
                // broadcast below has a real id to send.
                counterDAO.insert(counter);
                Web.flashSuccess(request, "Counter created.");
            }

            // Pushed so a wall-mounted board picks the change up without being touched.
            // Broadcast after the write, never before, so no screen can learn about a
            // change the database has not committed.
            QueueBroadcaster.counterUpdated(counter);

            redirect(request, response, "/admin/counters");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/admin/counters");
        }
    }
}
