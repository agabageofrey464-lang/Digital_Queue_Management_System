package com.digiq.servlet.staff;

import com.digiq.dao.CounterDAO;
import com.digiq.dao.TokenDAO;
import com.digiq.model.Counter;
import com.digiq.model.Token;
import com.digiq.model.User;
import com.digiq.servlet.BaseServlet;
// Writes the JSON reply the console's JavaScript reads.
import com.digiq.util.Json;
import com.digiq.util.Web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Resolves a scanned QR payload, or a typed token number, to a token.
 *
 * <p>Read-only on purpose. Scanning tells the staff member what the ticket is and
 * whether this counter may act on it; actually changing the status stays a separate,
 * deliberate press on {@code /staff/action}. Keeping the two apart means a stray
 * camera frame can never complete somebody's token by accident.</p>
 */
@WebServlet("/staff/scan")
public class ScanServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TokenDAO tokenDAO = new TokenDAO();
    private final CounterDAO counterDAO = new CounterDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        User user = Web.currentUser(request);

        // Either the UUID decoded from a QR code, or a token number typed by hand
        // when the camera is unavailable - the camera needs HTTPS or localhost.
        String code = Web.param(request, "code");

        if (code == null) {
            // 400, because the caller sent nothing to look up.
            Json.error(response, 400, "Scan a ticket or type a token number.");
            return;
        }

        try {
            // Tries the UUID first, then falls back to today's token numbers.
            Token token = tokenDAO.findByQrOrNumber(code);

            if (token == null) {
                // HTTP 200 with ok:false - the request was well formed, the ticket
                // just does not exist. The console shows this as a normal message
                // rather than an error state.
                Json.write(response, Json.map("ok", false, "message", "No ticket matches that code."));
                return;
            }

            // Which counter is scanning, so the reply can say whether it is theirs.
            Counter counter = counterDAO.findByStaffId(user.getId());

            // A ticket for Cash Deposit scanned at the Account Opening desk is a real
            // situation - the customer walked to the wrong counter - so it is reported
            // clearly instead of being treated as an error.
            boolean sameService = counter != null && counter.getServiceId() == token.getServiceId();

            Json.write(response, Json.map(
                    "ok", true,
                    "tokenId", token.getId(),
                    "tokenNumber", token.getTokenNumber(),
                    // Shown so the staff member can confirm the person in front of them.
                    "customerName", token.getCustomerName(),
                    "customerPhone", token.getCustomerPhone(),
                    "serviceName", token.getServiceName(),
                    "status", token.getStatus().name(),
                    "statusLabel", token.getStatus().getLabel(),
                    "priority", token.isPriority(),
                    "counterName", token.getCounterName(),
                    "issuedAt", token.getIssuedAt() == null ? null : token.getIssuedAt().toString(),
                    // Lets the console decide whether to offer the action buttons.
                    "servableHere", sameService,
                    // Null when there is nothing to warn about, so the console can
                    // simply test for its presence.
                    "message", sameService
                            ? null
                            : "This ticket is for " + token.getServiceName() + ", not your counter."));

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/staff/console");
        }
    }
}
