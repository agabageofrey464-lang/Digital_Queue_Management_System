package com.digiq.servlet.admin;

import com.digiq.dao.AnalyticsDAO;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Json;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * The analytics screen and the JSON behind its charts.
 *
 * <p>{@code /admin/analytics} renders the page with the first window already embedded;
 * {@code /admin/analytics/data?days=N} returns the same shape as JSON when the range
 * filter changes, so switching range never reloads the page.</p>
 */
@WebServlet({"/admin/analytics", "/admin/analytics/data"})
public class AnalyticsServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final AnalyticsDAO analyticsDAO = new AnalyticsDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int days = clamp(Web.intParam(request, "days", 14));
        boolean dataOnly = request.getRequestURI().endsWith("/data");

        try {
            Object payload = Json.map(
                    "days", days,
                    "tokensPerDay", analyticsDAO.tokensPerDay(days),
                    "statusBreakdown", analyticsDAO.statusBreakdown(days),
                    "waitVsService", analyticsDAO.waitVsServiceByService(days),
                    "peakHours", analyticsDAO.peakHours(days),
                    "counterPerformance", analyticsDAO.counterPerformance(days));

            if (dataOnly) {
                Json.write(response, payload);
                return;
            }

            request.setAttribute("stats", analyticsDAO.dashboard());
            request.setAttribute("days", days);
            request.setAttribute("analyticsJson", Json.stringify(payload));
            request.setAttribute("counterRows", analyticsDAO.counterPerformance(days));
            request.setAttribute("serviceRows", analyticsDAO.waitVsServiceByService(days));
            request.setAttribute("pageTitle", "Analytics");
            request.setAttribute("navActive", "analytics");
            render(request, response, "admin/analytics");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/admin/dashboard");
        }
    }

    /** The date-spine query supports at most 40 days, so the filter cannot exceed it. */
    private int clamp(int days) {
        if (days < 7) {
            return 7;
        }
        return Math.min(days, 30);
    }
}
