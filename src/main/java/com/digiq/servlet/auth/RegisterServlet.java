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

@WebServlet("/register")
public class RegisterServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (Web.currentUser(request) != null) {
            redirect(request, response, Web.currentUser(request).getRole().getLandingPage());
            return;
        }
        request.setAttribute("pageTitle", "Create an account");
        render(request, response, "auth/register");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String fullName = Web.param(request, "fullName");
        String email = Web.param(request, "email");
        String phone = Web.param(request, "phone");
        String password = Web.param(request, "password");
        String confirm = Web.param(request, "confirmPassword");

        String error = validate(fullName, email, password, confirm);
        if (error != null) {
            fail(request, response, error, fullName, email, phone);
            return;
        }

        try {
            User user = authService.register(fullName, email, phone, password);
            if (user == null) {
                fail(request, response, "An account already exists for that email address.",
                        fullName, email, phone);
                return;
            }
            HttpSession session = request.getSession(true);
            session.setAttribute(Web.SESSION_USER, user);
            Web.flashSuccess(request, "Your account is ready. You can book your first token now.");
            redirect(request, response, user.getRole().getLandingPage());
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/register");
        }
    }

    private String validate(String fullName, String email, String password, String confirm) {
        if (fullName == null || email == null || password == null) {
            return "Fill in your name, email address and password.";
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "That email address does not look valid.";
        }
        if (password.length() < 8) {
            return "Choose a password of at least 8 characters.";
        }
        if (!password.equals(confirm)) {
            return "The two passwords do not match.";
        }
        return null;
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String error,
                      String fullName, String email, String phone)
            throws ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("fullName", fullName);
        request.setAttribute("email", email);
        request.setAttribute("phone", phone);
        request.setAttribute("pageTitle", "Create an account");
        render(request, response, "auth/register");
    }
}
