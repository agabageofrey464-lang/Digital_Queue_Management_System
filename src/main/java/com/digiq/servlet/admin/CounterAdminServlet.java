package com.digiq.servlet.admin;

import com.digiq.dao.CounterDAO;
import com.digiq.dao.ServiceDAO;
import com.digiq.dao.UserDAO;
import com.digiq.model.Counter;
import com.digiq.model.CounterStatus;
import com.digiq.model.Role;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;
import com.digiq.websocket.QueueBroadcaster;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Counters, the service each one handles and the staff member behind it. */
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
            request.setAttribute("counters", counterDAO.findAll());
            request.setAttribute("services", serviceDAO.findAll());
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

            if (name == null || serviceId <= 0) {
                Web.flashError(request, "A counter needs a name and a service.");
                redirect(request, response, "/admin/counters");
                return;
            }

            Counter counter = new Counter();
            counter.setId(id);
            counter.setName(name);
            counter.setServiceId(serviceId);
            int staffId = Web.intParam(request, "staffId", 0);
            counter.setStaffId(staffId > 0 ? staffId : null);
            counter.setStatus(CounterStatus.from(Web.param(request, "status", "CLOSED")));

            if (id > 0) {
                counterDAO.update(counter);
                Web.flashSuccess(request, "Counter updated.");
            } else {
                counterDAO.insert(counter);
                Web.flashSuccess(request, "Counter created.");
            }
            // Keeps the public display board in step with the change.
            QueueBroadcaster.counterUpdated(counter);
            redirect(request, response, "/admin/counters");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/admin/counters");
        }
    }
}
