package com.digiq.servlet.customer;

import com.digiq.dao.TokenDAO;
import com.digiq.model.Token;
import com.digiq.model.User;
import com.digiq.service.PdfTokenService;
import com.digiq.service.QueueService;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Streams the printable A5 ticket. */
@WebServlet("/customer/token/pdf")
public class TokenPdfServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TokenDAO tokenDAO = new TokenDAO();
    private final QueueService queueService = new QueueService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = Web.currentUser(request);
        int id = Web.intParam(request, "id", 0);

        try {
            Token token = tokenDAO.findById(id);
            if (token == null || token.getCustomerId() != user.getId()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Token not found");
                return;
            }
            queueService.withPosition(token);
            byte[] pdf = PdfTokenService.build(token);

            response.setContentType("application/pdf");
            response.setContentLength(pdf.length);
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"DigiQ-" + token.getTokenNumber() + ".pdf\"");
            response.getOutputStream().write(pdf);
        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/tokens");
        }
    }
}
