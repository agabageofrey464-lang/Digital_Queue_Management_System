package com.digiq.servlet.customer;

import com.digiq.dao.TokenDAO;
import com.digiq.model.Token;
import com.digiq.model.User;
import com.digiq.service.QrCodeService;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Serves the token's QR code as a PNG for the on-screen ticket. */
@WebServlet("/customer/token/qr")
public class TokenQrServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TokenDAO tokenDAO = new TokenDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = Web.currentUser(request);
        int id = Web.intParam(request, "id", 0);
        int size = Math.min(Math.max(Web.intParam(request, "size", 260), 120), 600);

        try {
            Token token = tokenDAO.findById(id);
            if (token == null || token.getCustomerId() != user.getId()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Token not found");
                return;
            }
            byte[] png = QrCodeService.png(token.getQrPayload(), size);
            response.setContentType("image/png");
            response.setContentLength(png.length);
            // The payload never changes, so let the browser keep it.
            response.setHeader("Cache-Control", "private, max-age=3600");
            response.getOutputStream().write(png);
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/tokens");
        }
    }
}
