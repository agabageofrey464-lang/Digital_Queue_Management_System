package com.digiq.servlet.display;

import com.digiq.dao.CounterDAO;
import com.digiq.dao.ServiceDAO;
import com.digiq.dao.TokenDAO;
import com.digiq.model.Counter;
import com.digiq.model.Token;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Json;
import com.digiq.util.Web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The public "Now Serving" board for the waiting-area screen.
 *
 * <p>Deliberately unauthenticated: it shows token numbers and counters only, never a
 * customer name, so it can be left running on a wall-mounted display. The page renders
 * once and then updates over the WebSocket; {@code /board/data} is the snapshot it pulls
 * on connect and after any reconnect.</p>
 */
@WebServlet({"/board", "/board/data"})
public class DisplayBoardServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final CounterDAO counterDAO = new CounterDAO();
    private final TokenDAO tokenDAO = new TokenDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean dataOnly = request.getRequestURI().endsWith("/data");
        try {
            List<Counter> counters = counterDAO.findServing();
            List<Token> recent = tokenDAO.findRecentlyCalled(6);

            if (dataOnly || Web.wantsJson(request)) {
                Json.write(response, Json.map(
                        "ok", true,
                        "counters", counterRows(counters),
                        "recent", recentRows(recent)));
                return;
            }

            request.setAttribute("counters", counters);
            request.setAttribute("recent", recent);
            request.setAttribute("services", serviceDAO.findActiveWithQueueStats());
            request.setAttribute("pageTitle", "Now serving");
            render(request, response, "display/board");
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/");
        }
    }

    private List<Map<String, Object>> counterRows(List<Counter> counters) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Counter c : counters) {
            rows.add(Json.map(
                    "id", c.getId(),
                    "name", c.getName(),
                    "service", c.getServiceName(),
                    "status", c.getStatus().name(),
                    "token", c.getCurrentTokenNumber()));
        }
        return rows;
    }

    private List<Map<String, Object>> recentRows(List<Token> tokens) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Token t : tokens) {
            rows.add(Json.map(
                    "tokenNumber", t.getTokenNumber(),
                    "counterName", t.getCounterName(),
                    "service", t.getServiceName(),
                    "status", t.getStatus().name()));
        }
        return rows;
    }
}
