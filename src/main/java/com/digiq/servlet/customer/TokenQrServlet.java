package com.digiq.servlet.customer;

import com.digiq.dao.TokenDAO;
import com.digiq.model.Token;
import com.digiq.model.User;
// Turns the token's UUID into a PNG.
import com.digiq.service.QrCodeService;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Serves the token's QR code as a PNG, for the ticket shown on screen.
 *
 * <p>What the code actually encodes is the token's {@code qr_payload} UUID, never
 * its database id. A sequential id would let anyone generate a valid-looking code
 * for somebody else's ticket simply by counting; a random UUID cannot be guessed.</p>
 */
@WebServlet("/customer/token/qr")
public class TokenQrServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TokenDAO tokenDAO = new TokenDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        User user = Web.currentUser(request);
        int id = Web.intParam(request, "id", 0);

        // Clamp the requested size between 120 and 600 pixels. The parameter comes
        // straight from the URL, and without bounds somebody could ask for a
        // 20000-pixel image and tie up the server rendering it.
        int size = Math.min(Math.max(Web.intParam(request, "size", 260), 120), 600);

        try {
            Token token = tokenDAO.findById(id);

            // Same ownership rule as everywhere else in this package: a QR code is
            // effectively the key to the ticket, so it must not be fetchable for
            // someone else's token.
            if (token == null || token.getCustomerId() != user.getId()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Token not found");
                return;
            }

            byte[] png = QrCodeService.png(token.getQrPayload(), size);

            response.setContentType("image/png");
            response.setContentLength(png.length);

            // The payload never changes once issued, so the browser may keep it for
            // an hour. "private" stops any shared proxy from caching it, because the
            // image is specific to one customer.
            response.setHeader("Cache-Control", "private, max-age=3600");

            response.getOutputStream().write(png);

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/tokens");
        }
    }
}
