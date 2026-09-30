package com.digiq.servlet.staff;

import com.digiq.dao.CounterDAO;
import com.digiq.dao.TokenDAO;
import com.digiq.model.Counter;
import com.digiq.model.Token;
import com.digiq.model.User;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Json;
import com.digiq.util.Web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Resolves a scanned QR payload (or a typed token number) to a token.
 *
 * <p>The console calls this as the camera decodes a code; it only reports what the
 * ticket is and whether this counter may act on it. Changing the status stays a
 * separate, deliberate press on {@code /staff/action}.</p>
 */
@WebServlet("/staff/scan")
public class ScanServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TokenDAO tokenDAO = new TokenDAO();
    private final CounterDAO counterDAO = new CounterDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = Web.currentUser(request);
        String code = Web.param(request, "code");

        if (code == null) {
            Json.error(response, 400, "Scan a ticket or type a token number.");
            return;
        }

        try {
            Token token = tokenDAO.findByQrOrNumber(code);
            if (token == null) {
                Json.write(response, Json.map("ok", false, "message", "No ticket matches that code."));
                return;
            }

            Counter counter = counterDAO.findByStaffId(user.getId());
            boolean sameService = counter != null && counter.getServiceId() == token.getServiceId();

            Json.write(response, Json.map(
                    "ok", true,
                    "tokenId", token.getId(),
                    "tokenNumber", token.getTokenNumber(),
                    "customerName", token.getCustomerName(),
                    "customerPhone", token.getCustomerPhone(),
                    "serviceName", token.getServiceName(),
                    "status", token.getStatus().name(),
                    "statusLabel", token.getStatus().getLabel(),
                    "priority", token.isPriority(),
                    "counterName", token.getCounterName(),
                    "issuedAt", token.getIssuedAt() == null ? null : token.getIssuedAt().toString(),
                    "servableHere", sameService,
                    "message", sameService
                            ? null
                            : "This ticket is for " + token.getServiceName() + ", not your counter."));
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/staff/console");
        }
    }
}
