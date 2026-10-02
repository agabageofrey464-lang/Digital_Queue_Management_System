package com.digiq.servlet.auth;

// The account bean returned once registration succeeds.
import com.digiq.model.User;
// Encapsulates the "create a customer account" rule.
import com.digiq.service.AuthService;
// Supplies render(), redirect() and the shared SQL error handling.
import com.digiq.servlet.BaseServlet;
// Request helpers: trimmed parameters, flash messages, session access.
import com.digiq.util.Web;

// Thrown when forwarding to the JSP fails.
import javax.servlet.ServletException;
// Declares the URL this servlet answers on.
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// Used to sign the new account in immediately after it is created.
import javax.servlet.http.HttpSession;
import java.io.IOException;
// Any database failure surfaces as this.
import java.sql.SQLException;

/**
 * Public self-registration.
 *
 * <p>This is the only way an account is created without an administrator, and it
 * always produces a CUSTOMER. The role is fixed inside {@link AuthService}, not
 * read from the form, so adding {@code role=ADMIN} to the POST achieves nothing.</p>
 */
// Registers this servlet at /register. Deliberately outside the AuthFilter's
// protected prefixes, so anyone can reach it without signing in first.
@WebServlet("/register")
public class RegisterServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    // One shared instance: the service holds no per-request state, so this is safe
    // even though the container reuses a single servlet across many threads.
    private final AuthService authService = new AuthService();

    /** Shows the empty registration form. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Somebody already signed in has no use for this page, so bounce them
        // to whichever landing page their role owns.
        if (Web.currentUser(request) != null) {
            redirect(request, response, Web.currentUser(request).getRole().getLandingPage());
            return;
        }

        // Used by the layout for the <title> and the top bar heading.
        request.setAttribute("pageTitle", "Create an account");

        // Hand off to the JSP under /WEB-INF, which no one can request directly.
        render(request, response, "auth/register");
    }

    /** Processes the submitted form. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Web.param() trims and converts blank strings to null, so a field
        // containing only spaces is treated as missing rather than valid.
        String fullName = Web.param(request, "fullName");
        String email = Web.param(request, "email");
        String phone = Web.param(request, "phone");
        String password = Web.param(request, "password");
        String confirm = Web.param(request, "confirmPassword");

        // Server-side validation. The form also has HTML5 constraints, but those
        // are a convenience for the user - they can be bypassed trivially, so the
        // real check has to happen here.
        String error = validate(fullName, email, password, confirm);
        if (error != null) {
            // Re-show the form with the message and the values already typed,
            // so the user does not have to fill it all in again.
            fail(request, response, error, fullName, email, phone);
            return;
        }

        try {
            // Returns null when the address is already taken; the service hashes
            // the password and fixes the role to CUSTOMER.
            User user = authService.register(fullName, email, phone, password);

            if (user == null) {
                fail(request, response, "An account already exists for that email address.",
                        fullName, email, phone);
                return;
            }

            // Sign the new account straight in, so registration leads directly to
            // booking rather than dropping the user back on a login form.
            HttpSession session = request.getSession(true);
            session.setAttribute(Web.SESSION_USER, user);

            // Shown on the page they land on.
            Web.flashSuccess(request, "Your account is ready. You can book your first token now.");

            // CUSTOMER, so this resolves to /customer/home.
            redirect(request, response, user.getRole().getLandingPage());

        } catch (SQLException ex) {
            // Logs the stack trace server-side and shows the user a plain message,
            // rather than leaking database details onto the page.
            handleSqlError(request, response, ex, "/register");
        }
    }

    /**
     * Checks the submitted values.
     *
     * @return the first problem found, or null when everything is acceptable
     */
    private String validate(String fullName, String email, String password, String confirm) {
        // Phone is optional, so it is not checked here.
        if (fullName == null || email == null || password == null) {
            return "Fill in your name, email address and password.";
        }

        // A deliberately loose pattern: something, an @, something, a dot, something.
        // Anything stricter starts rejecting addresses that are genuinely valid.
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "That email address does not look valid.";
        }

        // Matches the minimum enforced in the admin console, so the two cannot drift.
        if (password.length() < 8) {
            return "Choose a password of at least 8 characters.";
        }

        // Catches a typo in a field the user cannot read back.
        if (!password.equals(confirm)) {
            return "The two passwords do not match.";
        }

        // Null means "no problem found".
        return null;
    }

    /** Redisplays the form with an error and the values already entered. */
    private void fail(HttpServletRequest request, HttpServletResponse response, String error,
                      String fullName, String email, String phone)
            throws ServletException, IOException {

        // Read back by the JSP to show the red message box.
        request.setAttribute("error", error);

        // Re-populate the fields. The password boxes are deliberately NOT
        // repopulated - browsers and users expect those to clear.
        request.setAttribute("fullName", fullName);
        request.setAttribute("email", email);
        request.setAttribute("phone", phone);
        request.setAttribute("pageTitle", "Create an account");

        // Forward rather than redirect, so the attributes above survive.
        render(request, response, "auth/register");
    }
}
