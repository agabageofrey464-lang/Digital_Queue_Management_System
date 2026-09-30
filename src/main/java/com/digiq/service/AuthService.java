package com.digiq.service;

import com.digiq.dao.UserDAO;
import com.digiq.model.Role;
import com.digiq.model.User;
import com.digiq.util.PasswordUtil;

import java.sql.SQLException;

/** Sign-in and self-registration. */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * @return the authenticated user, or null when the credentials are wrong or the
     *         account has been deactivated. The caller is deliberately not told which,
     *         so the login form cannot be used to discover valid addresses.
     */
    public User authenticate(String email, String password) throws SQLException {
        if (email == null || password == null) {
            return null;
        }
        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null || !user.isActive()) {
            return null;
        }
        if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            return null;
        }
        user.setPasswordHash(null);
        return user;
    }

    /**
     * Registers a customer account. Self-registration is always CUSTOMER; staff and
     * admin accounts are created from the admin console only.
     */
    public User register(String fullName, String email, String phone, String password) throws SQLException {
        String normalised = email.trim().toLowerCase();
        if (userDAO.emailExists(normalised)) {
            return null;
        }
        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalised);
        user.setPhone(phone);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        userDAO.insert(user);
        user.setPasswordHash(null);
        return user;
    }
}
