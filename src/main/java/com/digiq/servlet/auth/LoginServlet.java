package com.digiq.servlet.auth;

import com.digiq.model.User;
// Checks the password and returns the account, or null.
import com.digiq.service.AuthService;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// The session is where the signed-in user is stored for later requests.
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * The single sign-in point for all three roles.
 *
 * <p>One form rather than three. The role on the account decides where the user
 * lands, so nobody has to know which "portal" they belong to, and there is only one
 * place where authentication can go wrong.</p>
 */
// Public: this is deliberately outside the AuthFilter's protected prefixes.
@WebServlet("/login")
public class LoginServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();

    /** Shows the sign-in form. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User current = Web.currentUser(request);

        // Already signed in, so skip the form entirely.
        if (current != null) {
            redirect(request, response, current.getRole().getLandingPage());
            return;
        }

        request.setAttribute("pageTitle", "Sign in");
        render(request, response, "auth/login");
    }

    /** Checks the submitted credentials. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Named "email" for historical reasons, but it accepts a plain username too -
        // the form field is type="text" so the browser does not reject one.
        String email = Web.param(request, "email");
        String password = Web.param(request, "password");

        // Either field blank. Handled before touching the database.
        if (email == null || password == null) {
            request.setAttribute("error", "Enter both your email address and your password.");
            // Put the identifier back so only the password has to be retyped.
            request.setAttribute("email", email);
            request.setAttribute("pageTitle", "Sign in");
            render(request, response, "auth/login");
            return;
        }

        try {
            // Null covers three different failures: no such account, wrong password,
            // and account deactivated.
            User user = authService.authenticate(email, password);

            if (user == null) {
                // One message for all three. Saying "no such account" would turn this
                // form into a tool for discovering which addresses are registered.
                request.setAttribute("error", "Those credentials did not match an active account.");
                request.setAttribute("email", email);
                request.setAttribute("pageTitle", "Sign in");
                render(request, response, "auth/login");
                return;
            }

            // Throw away any session that existed before the sign-in. Without this, an
            // attacker who fixed a session id in the victim's browser beforehand would
            // still hold a valid handle on the now-authenticated session.
            HttpSession old = request.getSession(false);
            if (old != null) {
                old.invalidate();
            }

            // A genuinely new session, created only now that identity is proven.
            HttpSession session = request.getSession(true);

            // Everything downstream - AuthFilter, every servlet - reads the user from here.
            session.setAttribute(Web.SESSION_USER, user);

            // One hour of inactivity. Long enough for a shift at a counter, short
            // enough that an unattended browser does not stay signed in all day.
            session.setMaxInactiveInterval(60 * 60);

            // Set by AuthFilter when an unauthenticated user asked for a specific page,
            // so they resume where they were aiming instead of a generic landing page.
            String wanted = (String) session.getAttribute("redirectAfterLogin");
            // Read once, then cleared, so it cannot redirect a later sign-in.
            session.removeAttribute("redirectAfterLogin");

            Web.flashSuccess(request, "Welcome back, " + user.getFullName() + ".");

            // Their original destination if there was one, otherwise the role's home.
            redirect(request, response, wanted != null ? wanted : user.getRole().getLandingPage());

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/login");
        }
    }
}
