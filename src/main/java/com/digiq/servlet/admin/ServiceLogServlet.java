package com.digiq.servlet.admin;

import com.digiq.dao.ServiceDAO;
import com.digiq.dao.ServiceLogDAO;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * The audit trail: every status change, with who made it and when.
 *
 * <p>Read-only by design. There is no edit or delete anywhere in this class, because
 * a trail that can be altered is not evidence of anything. Rows are written inside
 * the same transaction as the change they record, so the log cannot drift from the
 * tokens table even if the application crashes mid-request.</p>
 */
@WebServlet("/admin/logs")
public class ServiceLogServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ServiceLogDAO logDAO = new ServiceLogDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Null means no action filter - ISSUED, CALLED, COMPLETED and so on.
        String action = Web.param(request, "action");

        // 0 means every service.
        int serviceId = Web.intParam(request, "serviceId", 0);

        try {
            // Unlike the live-queue page this is not limited to today, so the cap
            // matters more: 250 rows is the ceiling on one page of history.
            request.setAttribute("logs", logDAO.search(action, serviceId, 250));

            // For the service filter dropdown.
            request.setAttribute("services", serviceDAO.findAll());

            // Echoed back so the filters keep their selection after submitting.
            request.setAttribute("selectedAction", action);
            request.setAttribute("selectedService", serviceId);

            request.setAttribute("pageTitle", "Service logs");
            request.setAttribute("navActive", "logs");

            render(request, response, "admin/logs");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/admin/dashboard");
        }
    }
}
