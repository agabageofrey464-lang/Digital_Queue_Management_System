package com.digiq.servlet.auth;

// BaseServlet supplies the shared redirect() helper used below.
import com.digiq.servlet.BaseServlet;
// Web holds the session-attribute names and the flash-message helpers.
import com.digiq.util.Web;

// Maps this class to a URL without needing an entry in web.xml.
import javax.servlet.annotation.WebServlet;
// The incoming request, which is how we reach the current session.
import javax.servlet.http.HttpServletRequest;
// The outgoing response, used here only to send a redirect.
import javax.servlet.http.HttpServletResponse;
// The session object that actually holds the signed-in user.
import javax.servlet.http.HttpSession;
// Thrown if the redirect cannot be written back to the browser.
import java.io.IOException;

/**
 * Signs the current user out.
 *
 * <p>Destroying the whole session rather than just removing the user attribute
 * means anything else cached against that session (flash messages, the
 * "page you wanted before logging in" marker) is discarded at the same time,
 * so nothing leaks from one user's session into the next.</p>
 */
// Registers this servlet at /logout.
@WebServlet("/logout")
public class LogoutServlet extends BaseServlet {

    // Required because HttpServlet is Serializable; fixes the class version.
    private static final long serialVersionUID = 1L;

    /** Handles a normal link click, e.g. the "Sign out" button in the top bar. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Pass false so we do NOT create a session just to destroy it;
        // a visitor who was never signed in simply gets null here.
        HttpSession session = request.getSession(false);

        // Only invalidate when a session actually exists, otherwise this throws.
        if (session != null) {
            // Wipes every attribute and makes the old session id useless,
            // which is what actually ends the sign-in.
            session.invalidate();
        }

        // Queue a confirmation. This creates a NEW session, which is safe
        // because the old one is already gone and this one holds no identity.
        Web.flashSuccess(request, "You have been signed out.");

        // Send the browser to the login page to read that message.
        redirect(request, response, "/login");
    }

    /**
     * Handles a form POST.
     *
     * <p>Present so that signing out can be done with a form instead of a link.
     * A link is a GET, which browsers and crawlers may prefetch; a POST cannot be
     * triggered that way, so a form is the safer option if that ever matters.</p>
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // The behaviour is identical, so delegate rather than duplicate it.
        doGet(request, response);
    }
}
