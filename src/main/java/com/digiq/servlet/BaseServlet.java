package com.digiq.servlet;

import com.digiq.util.Json;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Shared plumbing: view rendering, redirects and a single place to handle SQL failures. */
public abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Forwards to a JSP under /WEB-INF/views, which is unreachable by direct URL. */
    protected void render(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        request.setAttribute("contextPath", request.getContextPath());
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }

    protected void redirect(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    /**
     * Turns a database failure into something the user can act on, and keeps the stack
     * trace in the Tomcat log rather than on the screen.
     */
    protected void handleSqlError(HttpServletRequest request, HttpServletResponse response,
                                  SQLException ex, String fallbackPath)
            throws IOException {
        getServletContext().log("DigiQ database error at " + request.getRequestURI(), ex);
        if (Web.wantsJson(request)) {
            Json.error(response, 500, "The system could not reach the database. Please try again.");
            return;
        }
        Web.flashError(request, "The system could not reach the database. Please try again.");
        redirect(request, response, fallbackPath);
    }
}
