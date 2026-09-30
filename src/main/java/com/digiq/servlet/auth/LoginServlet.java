package com.digiq.servlet.auth;

import com.digiq.model.User;
import com.digiq.service.AuthService;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User current = Web.currentUser(request);
        if (current != null) {
            redirect(request, response, current.getRole().getLandingPage());
            return;
        }
        request.setAttribute("pageTitle", "Sign in");
        render(request, response, "auth/login");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = Web.param(request, "email");
        String password = Web.param(request, "password");

        if (email == null || password == null) {
            request.setAttribute("error", "Enter both your email address and your password.");
            request.setAttribute("email", email);
            request.setAttribute("pageTitle", "Sign in");
            render(request, response, "auth/login");
            return;
        }

        try {
            User user = authService.authenticate(email, password);
            if (user == null) {
                request.setAttribute("error", "Those credentials did not match an active account.");
                request.setAttribute("email", email);
                request.setAttribute("pageTitle", "Sign in");
                render(request, response, "auth/login");
                return;
            }

            // New session on login - stops a pre-existing session id being reused.
            HttpSession old = request.getSession(false);
            if (old != null) {
                old.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute(Web.SESSION_USER, user);
            session.setMaxInactiveInterval(60 * 60);

            String wanted = (String) session.getAttribute("redirectAfterLogin");
            session.removeAttribute("redirectAfterLogin");

            Web.flashSuccess(request, "Welcome back, " + user.getFullName() + ".");
            redirect(request, response, wanted != null ? wanted : user.getRole().getLandingPage());
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/login");
        }
    }
}
