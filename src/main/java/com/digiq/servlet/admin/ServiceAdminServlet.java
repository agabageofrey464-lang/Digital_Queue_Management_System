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

/**
 * Create, edit and retire the services customers can queue for.
 *
 * <p>One servlet handling list, create, update and delete. They share a URL and a
 * redirect target, and separating them would mean four classes that each do three
 * lines of work.</p>
 */
// Under /admin/*, so AuthFilter has already confirmed an ADMIN is signed in.
@WebServlet("/admin/services")
public class ServiceAdminServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ServiceDAO serviceDAO = new ServiceDAO();

    /** Lists every service, including retired ones. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            // findAll rather than findActive - an admin needs to see what has been
            // retired in order to bring it back.
            request.setAttribute("services", serviceDAO.findAll());
            request.setAttribute("pageTitle", "Services");
            request.setAttribute("navActive", "services");

            render(request, response, "admin/services");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/admin/dashboard");
        }
    }

    /** Handles both the save (create or update) and the delete. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Defaults to "save", so a form missing the field still does the safe thing
        // rather than falling through to a delete.
        String action = Web.param(request, "action", "save");

        try {
            if ("delete".equals(action)) {
                // The foreign keys cascade, so this also removes the service's counters
                // and token history. The form asks for confirmation before posting.
                serviceDAO.delete(Web.intParam(request, "id", 0));
                Web.flashSuccess(request, "Service removed.");
                redirect(request, response, "/admin/services");
                return;
            }

            // 0 means "new". Anything above zero is an edit of that row.
            int id = Web.intParam(request, "id", 0);
            String name = Web.param(request, "name");
            String code = Web.param(request, "code");

            // Both are required: the name is what customers see, the code becomes the
            // token prefix, and neither can be derived from the other.
            if (name == null || code == null) {
                Web.flashError(request, "A service needs both a name and a token code.");
                redirect(request, response, "/admin/services");
                return;
            }

            // Token numbers read ACC-0001, so the code is stored uppercase and the
            // form's styling is not relied on to enforce it.
            code = code.toUpperCase();

            // Checked here so the admin gets a clear message. The UNIQUE constraint on
            // the column is the real guarantee - this is the friendly version of it.
            // Passing the row's own id excludes it, so saving an unchanged row works.
            if (serviceDAO.codeExists(code, id)) {
                Web.flashError(request, "The token code " + code + " is already in use.");
                redirect(request, response, "/admin/services");
                return;
            }

            // Built here rather than loaded and mutated: every editable field is on the
            // form, so a fresh object cannot carry stale values.
            Service service = new Service();
            service.setId(id);
            service.setName(name);
            service.setCode(code);
            service.setDescription(Web.param(request, "description"));

            // At least one minute, or the estimated-wait arithmetic produces zero
            // for every queue regardless of length.
            service.setAvgServiceMinutes(Math.max(1, Web.intParam(request, "avgServiceMinutes", 10)));

            // Unticked checkboxes are not submitted at all, so absence means inactive.
            service.setActive(Web.boolParam(request, "active"));

            if (id > 0) {
                serviceDAO.update(service);
                Web.flashSuccess(request, "Service updated.");
            } else {
                serviceDAO.insert(service);
                Web.flashSuccess(request, "Service created.");
            }

            // Post/Redirect/Get, so refreshing the list cannot repeat the write.
            redirect(request, response, "/admin/services");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/admin/services");
        }
    }
}
