package com.digiq.servlet.admin;

import com.digiq.dao.UserDAO;
import com.digiq.model.Role;
import com.digiq.model.User;
import com.digiq.servlet.BaseServlet;
// BCrypt hashing - the servlet never stores a plain password.
import com.digiq.util.PasswordUtil;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Staff and customer accounts.
 *
 * <p>The only place staff and administrator accounts can be created - public
 * registration always produces a customer. Three guards protect an admin from
 * locking themselves out, and they are the reason this class is longer than the
 * other CRUD screens.</p>
 */
@WebServlet("/admin/users")
public class UserAdminServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            // Ordered by role then name, so the three groups read together.
            request.setAttribute("users", userDAO.findAll());

            // Keyed by role NAME, not the enum, because EL cannot look up an enum
            // key with the string literal ${roleCounts['ADMIN']}.
            request.setAttribute("roleCounts", userDAO.countByRole());

            request.setAttribute("pageTitle", "Users");
            request.setAttribute("navActive", "users");

            render(request, response, "admin/users");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/admin/dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String action = Web.param(request, "action", "save");

        // Needed by the self-protection guards below.
        User current = Web.currentUser(request);

        try {
            int id = Web.intParam(request, "id", 0);

            if ("delete".equals(action)) {
                // GUARD 1: deleting your own account would end your session and, if you
                // were the last admin, leave the console permanently unreachable.
                if (id == current.getId()) {
                    Web.flashError(request, "You cannot delete the account you are signed in with.");
                } else {
                    userDAO.delete(id);
                    Web.flashSuccess(request, "Account removed.");
                }
                redirect(request, response, "/admin/users");
                return;
            }

            if ("resetPassword".equals(action)) {
                String password = Web.param(request, "password");

                // Same minimum as public registration, so the two cannot drift apart.
                if (password == null || password.length() < 8) {
                    Web.flashError(request, "The new password must be at least 8 characters.");
                } else {
                    // Only the hash is stored; the plain value is never written anywhere.
                    userDAO.updatePassword(id, PasswordUtil.hash(password));
                    Web.flashSuccess(request, "Password reset.");
                }
                redirect(request, response, "/admin/users");
                return;
            }

            String fullName = Web.param(request, "fullName");
            String email = Web.param(request, "email");

            if (fullName == null || email == null) {
                Web.flashError(request, "A user needs a name and an email address.");
                redirect(request, response, "/admin/users");
                return;
            }

            // Stored lowercase so sign-in is case-insensitive, matching what
            // AuthService does when it looks the account up.
            email = email.toLowerCase();

            User user = new User();
            user.setId(id);
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(Web.param(request, "phone"));

            // Unrecognised values fall back to CUSTOMER, the least privileged role.
            user.setRole(Role.from(Web.param(request, "role", "CUSTOMER")));
            user.setActive(Web.boolParam(request, "active"));

            if (id > 0) {
                // GUARD 2: demoting yourself would take effect immediately and lock you
                // out of this very page.
                if (id == current.getId() && user.getRole() != Role.ADMIN) {
                    Web.flashError(request, "You cannot remove your own administrator role.");
                    redirect(request, response, "/admin/users");
                    return;
                }

                // No password here: editing details must not silently change the
                // password, which is why resetting it is a separate action.
                userDAO.update(user);
                Web.flashSuccess(request, "Account updated.");

            } else {
                // GUARD 3: checked explicitly so the admin sees a readable message
                // rather than a UNIQUE constraint violation.
                if (userDAO.emailExists(email)) {
                    Web.flashError(request, "That email address is already registered.");
                    redirect(request, response, "/admin/users");
                    return;
                }

                String password = Web.param(request, "password");

                // A new account with no password could never be signed into, so this
                // is required rather than optional.
                if (password == null || password.length() < 8) {
                    Web.flashError(request, "Set a password of at least 8 characters for the new account.");
                    redirect(request, response, "/admin/users");
                    return;
                }

                user.setPasswordHash(PasswordUtil.hash(password));
                userDAO.insert(user);
                Web.flashSuccess(request, "Account created.");
            }

            redirect(request, response, "/admin/users");

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/admin/users");
        }
    }
}
