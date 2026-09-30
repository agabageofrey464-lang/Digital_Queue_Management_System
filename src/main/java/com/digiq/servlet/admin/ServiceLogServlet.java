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

/** The audit trail - every status change, with who made it and when. */
@WebServlet("/admin/logs")
public class ServiceLogServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ServiceLogDAO logDAO = new ServiceLogDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = Web.param(request, "action");
        int serviceId = Web.intParam(request, "serviceId", 0);

        try {
            request.setAttribute("logs", logDAO.search(action, serviceId, 250));
            request.setAttribute("services", serviceDAO.findAll());
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
