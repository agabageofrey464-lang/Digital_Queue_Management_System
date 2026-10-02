package com.digiq.servlet.customer;

import com.digiq.dao.TokenDAO;
import com.digiq.model.Token;
import com.digiq.model.User;
// Builds the A5 ticket, QR code included.
import com.digiq.service.PdfTokenService;
import com.digiq.service.QueueService;
import com.digiq.servlet.BaseServlet;
import com.digiq.util.Web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Streams the printable A5 ticket.
 *
 * <p>Generated per request rather than stored. The PDF takes a few milliseconds to
 * build, and generating it fresh means the position and counter printed on it are
 * the ones true at the moment of download, not whenever it was first created.</p>
 */
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

            // Same ownership rule as the ticket page. Without it, changing the id in
            // the URL would hand over another customer's name and phone number in a
            // downloadable file.
            if (token == null || token.getCustomerId() != user.getId()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Token not found");
                return;
            }

            // So the printed ticket shows the current position, not a stale one.
            queueService.withPosition(token);

            // Built entirely in memory - small enough that writing a temp file
            // would cost more than it saves.
            byte[] pdf = PdfTokenService.build(token);

            // Tells the browser this is a PDF rather than something to render as text.
            response.setContentType("application/pdf");

            // Letting the browser know the size up front gives a real progress bar
            // and avoids chunked transfer for a file this small.
            response.setContentLength(pdf.length);

            // "attachment" forces a download rather than opening in a viewer tab, and
            // names the file after the token so a printed stack stays sortable.
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"DigiQ-" + token.getTokenNumber() + ".pdf\"");

            // Binary, so the output stream rather than the character writer.
            response.getOutputStream().write(pdf);

        } catch (SQLException ex) {
            handleSqlError(request, response, ex, "/customer/tokens");
        }
    }
}
