package com.digiq.servlet.admin;

import com.digiq.dao.ServiceDAO;
import com.digiq.dao.TokenDAO;
import com.digiq.model.TokenStatus;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Every token issued today, filterable by service and status.
 *
 * <p>Scoped to today on purpose. This is an operational view - "what is happening
 * on the floor right now" - and the full history belongs on the service-logs page.
 * Limiting it by date is also what keeps the query fast as the table grows.</p>
 */
@WebServlet("/admin/tokens")
public class LiveQueueServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TokenDAO tokenDAO = new TokenDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Null when "All statuses" is selected.
        String statusParam = Web.param(request, "status");

        // 0 means "All services".
        int serviceId = Web.intParam(request, "serviceId", 0);

        // Only converted when something was actually chosen, so that null can keep
        // meaning "do not filter on status at all".
        TokenStatus status = statusParam == null ? null : TokenStatus.from(statusParam);

        try {
            // 200 rows is comfortably more than a branch issues in a day while still
            // bounding the page if the data is unusual.
            request.setAttribute("tokens", tokenDAO.findToday(status, serviceId, 200));

            // Populates the service filter dropdown.
            request.setAttribute("services", serviceDAO.findAll());

            // The enum itself drives the status dropdown, so adding a status to the
            // enum adds it to the filter with no change here.
            request.setAttribute("statuses", TokenStatus.values());

            // Echoed back so the dropdowns stay on the user's choice after reload.
            request.setAttribute("selectedStatus", statusParam);
            request.setAttribute("selectedService", serviceId);

            request.setAttribute("pageTitle", "Live queue");
            request.setAttribute("navActive", "tokens");

            render(request, response, "admin/tokens");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/admin/dashboard");
        }
    }
}
