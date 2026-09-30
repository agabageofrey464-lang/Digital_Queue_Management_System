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

/** Every token issued today, filterable by service and status. */
@WebServlet("/admin/tokens")
public class LiveQueueServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TokenDAO tokenDAO = new TokenDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String statusParam = Web.param(request, "status");
        int serviceId = Web.intParam(request, "serviceId", 0);
        TokenStatus status = statusParam == null ? null : TokenStatus.from(statusParam);

        try {
            request.setAttribute("tokens", tokenDAO.findToday(status, serviceId, 200));
            request.setAttribute("services", serviceDAO.findAll());
            request.setAttribute("statuses", TokenStatus.values());
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
