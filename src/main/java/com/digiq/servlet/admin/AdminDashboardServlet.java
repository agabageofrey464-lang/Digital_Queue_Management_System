package com.digiq.servlet.admin;

import com.digiq.dao.AnalyticsDAO;
import com.digiq.dao.CounterDAO;
import com.digiq.dao.ServiceDAO;
import com.digiq.dao.ServiceLogDAO;
import com.digiq.dao.TokenDAO;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Json;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Operational overview: today's numbers, counter states and the live activity feed. */
@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final AnalyticsDAO analyticsDAO = new AnalyticsDAO();
    private final CounterDAO counterDAO = new CounterDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final TokenDAO tokenDAO = new TokenDAO();
    private final ServiceLogDAO logDAO = new ServiceLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("stats", analyticsDAO.dashboard());
            request.setAttribute("counters", counterDAO.findAll());
            request.setAttribute("services", serviceDAO.findActiveWithQueueStats());
            request.setAttribute("recentTokens", tokenDAO.findToday(null, 0, 10));
            request.setAttribute("activity", logDAO.findRecent(10));

            // Serialised here so the dashboard sparkline needs no extra round trip.
            request.setAttribute("trendJson", Json.stringify(analyticsDAO.tokensPerDay(14)));

            request.setAttribute("pageTitle", "Dashboard");
            request.setAttribute("navActive", "dashboard");
            render(request, response, "admin/dashboard");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/");
        }
    }
}
