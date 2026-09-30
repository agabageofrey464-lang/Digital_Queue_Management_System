package com.digiq.servlet.admin;

import com.digiq.dao.UserDAO;
import com.digiq.model.Role;
import com.digiq.model.User;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.PasswordUtil;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Staff and customer accounts. */
@WebServlet("/admin/users")
public class UserAdminServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("users", userDAO.findAll());
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
        User current = Web.currentUser(request);

        try {
            int id = Web.intParam(request, "id", 0);

            if ("delete".equals(action)) {
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
                if (password == null || password.length() < 8) {
                    Web.flashError(request, "The new password must be at least 8 characters.");
                } else {
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
            email = email.toLowerCase();

            User user = new User();
            user.setId(id);
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(Web.param(request, "phone"));
            user.setRole(Role.from(Web.param(request, "role", "CUSTOMER")));
            user.setActive(Web.boolParam(request, "active"));

            if (id > 0) {
                if (id == current.getId() && user.getRole() != Role.ADMIN) {
                    // Otherwise the last admin can lock themselves out of the console.
                    Web.flashError(request, "You cannot remove your own administrator role.");
                    redirect(request, response, "/admin/users");
                    return;
                }
                userDAO.update(user);
                Web.flashSuccess(request, "Account updated.");
            } else {
                if (userDAO.emailExists(email)) {
                    Web.flashError(request, "That email address is already registered.");
                    redirect(request, response, "/admin/users");
                    return;
                }
                String password = Web.param(request, "password");
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
