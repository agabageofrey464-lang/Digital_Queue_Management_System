package com.digiq.filter;

import com.digiq.model.Role;
import com.digiq.model.User;
import com.digiq.util.Json;
import com.digiq.util.Web;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Gate for the three role areas.
 *
 * <p>The URL prefix decides the role required, so a new page under {@code /admin/}
 * is protected the moment it is added rather than when somebody remembers to guard
 * it. An unauthenticated visitor is bounced to the login form with the page they
 * wanted remembered; a signed-in user in the wrong area gets 403 rather than a
 * redirect loop.</p>
 */
@WebFilter(filterName = "authFilter", urlPatterns = {"/admin/*", "/staff/*", "/customer/*"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        Role required = requiredRole(path);

        User user = Web.currentUser(request);
        if (user == null) {
            deny(request, response, 401, "Please sign in to continue.");
            return;
        }
        if (required != null && user.getRole() != required) {
            deny(request, response, 403, "Your account does not have access to that area.");
            return;
        }
        chain.doFilter(req, res);
    }

    private Role requiredRole(String path) {
        if (path.startsWith("/admin/")) {
            return Role.ADMIN;
        }
        if (path.startsWith("/staff/")) {
            return Role.STAFF;
        }
        if (path.startsWith("/customer/")) {
            return Role.CUSTOMER;
        }
        return null;
    }

    private void deny(HttpServletRequest request, HttpServletResponse response, int status, String message)
            throws IOException {
        if (Web.wantsJson(request)) {
            Json.error(response, status, message);
            return;
        }
        if (status == 401) {
            String wanted = request.getRequestURI().substring(request.getContextPath().length());
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                request.getSession().setAttribute("redirectAfterLogin", wanted);
            }
            Web.flashError(request, message);
            response.sendRedirect(request.getContextPath() + "/login");
        } else {
            Web.flashError(request, message);
            response.sendRedirect(request.getContextPath() + "/");
        }
    }
}
