package com.digiq.servlet;

import com.digiq.dao.CounterDAO;
import com.digiq.dao.ServiceDAO;
import com.digiq.model.User;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Public landing page: what the branch offers and how busy it is right now. */
@WebServlet("")
public class HomeServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final CounterDAO counterDAO = new CounterDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Web.currentUser(request);
        if (user != null) {
            redirect(request, response, user.getRole().getLandingPage());
            return;
        }
        try {
            request.setAttribute("services", serviceDAO.findActiveWithQueueStats());
            request.setAttribute("openCounters", counterDAO.countOpen());
            request.setAttribute("pageTitle", "Welcome");
            render(request, response, "home");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/login");
        }
    }
}
