package com.digiq.util;

import com.digiq.model.Role;
import com.digiq.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** Request/session conveniences shared by the servlets. */
public final class Web {

    public static final String SESSION_USER = "authUser";
    public static final String FLASH_SUCCESS = "flashSuccess";
    public static final String FLASH_ERROR = "flashError";

    private Web() {
    }

    public static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute(SESSION_USER);
    }

    public static boolean hasRole(HttpServletRequest request, Role role) {
        User user = currentUser(request);
        return user != null && user.getRole() == role;
    }

    /** Reads a trimmed parameter, returning null rather than an empty string. */
    public static String param(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    public static String param(HttpServletRequest request, String name, String fallback) {
        String value = param(request, name);
        return value == null ? fallback : value;
    }

    public static int intParam(HttpServletRequest request, String name, int fallback) {
        String value = param(request, name);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static boolean boolParam(HttpServletRequest request, String name) {
        String value = param(request, name);
        return value != null
                && ("on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value));
    }

    /** Stores a one-shot message shown on the next page the user lands on. */
    public static void flashSuccess(HttpServletRequest request, String message) {
        request.getSession().setAttribute(FLASH_SUCCESS, message);
    }

    public static void flashError(HttpServletRequest request, String message) {
        request.getSession().setAttribute(FLASH_ERROR, message);
    }

    /** True when the caller expects JSON (our fetch() calls set this header). */
    public static boolean wantsJson(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "fetch".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"));
    }
}
