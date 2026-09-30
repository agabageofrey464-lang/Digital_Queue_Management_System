package com.digiq.servlet.admin;

import com.digiq.dao.ServiceDAO;
import com.digiq.model.Service;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Create, edit, retire the services customers can queue for. */
@WebServlet("/admin/services")
public class ServiceAdminServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ServiceDAO serviceDAO = new ServiceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("services", serviceDAO.findAll());
            request.setAttribute("pageTitle", "Services");
            request.setAttribute("navActive", "services");
            render(request, response, "admin/services");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/admin/dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = Web.param(request, "action", "save");
        try {
            if ("delete".equals(action)) {
                serviceDAO.delete(Web.intParam(request, "id", 0));
                Web.flashSuccess(request, "Service removed.");
                redirect(request, response, "/admin/services");
                return;
            }

            int id = Web.intParam(request, "id", 0);
            String name = Web.param(request, "name");
            String code = Web.param(request, "code");

            if (name == null || code == null) {
                Web.flashError(request, "A service needs both a name and a token code.");
                redirect(request, response, "/admin/services");
                return;
            }
            code = code.toUpperCase();
            if (serviceDAO.codeExists(code, id)) {
                Web.flashError(request, "The token code " + code + " is already in use.");
                redirect(request, response, "/admin/services");
                return;
            }

            Service service = new Service();
            service.setId(id);
            service.setName(name);
            service.setCode(code);
            service.setDescription(Web.param(request, "description"));
            service.setAvgServiceMinutes(Math.max(1, Web.intParam(request, "avgServiceMinutes", 10)));
            service.setActive(Web.boolParam(request, "active"));

            if (id > 0) {
                serviceDAO.update(service);
                Web.flashSuccess(request, "Service updated.");
            } else {
                serviceDAO.insert(service);
                Web.flashSuccess(request, "Service created.");
            }
            redirect(request, response, "/admin/services");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/admin/services");
        }
    }
}
