package com.digiq.service;

import com.digiq.model.Token;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Builds the printable A5 ticket a customer downloads after booking.
 *
 * <p>The layout deliberately mirrors the on-screen token card: brand band, oversized
 * token number, QR code, then the details table - so a printed ticket and the web page
 * are recognisably the same artefact.</p>
 */
public final class PdfTokenService {

    private static final Color NAVY = new Color(0x0D, 0x36, 0x6B);
    private static final Color BLUE = new Color(0x2A, 0x78, 0xD6);
    private static final Color INK = new Color(0x0B, 0x0B, 0x0B);
    private static final Color MUTED = new Color(0x89, 0x87, 0x81);
    private static final Color HAIRLINE = new Color(0xE1, 0xE0, 0xD9);

    private static final SimpleDateFormat STAMP = new SimpleDateFormat("EEE dd MMM yyyy, HH:mm");

    private PdfTokenService() {
    }

    public static byte[] build(Token token) throws IOException {
        Document doc = new Document(PageSize.A5, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(doc, out);
            doc.addTitle("DigiQ Token " + token.getTokenNumber());
            doc.addCreator("DigiQ - Digital Queue Management System");
            doc.open();

            doc.add(brandBand());
            doc.add(spacer(18));
            doc.add(centered("YOUR TOKEN NUMBER", font(9, Font.BOLD, MUTED)));
            doc.add(spacer(4));
            doc.add(centered(token.getTokenNumber(), font(42, Font.BOLD, NAVY)));
            doc.add(spacer(2));
            doc.add(centered(token.getServiceName(), font(13, Font.NORMAL, INK)));

            if (token.isPriority()) {
                doc.add(spacer(6));
                doc.add(centered("PRIORITY SERVICE", font(9, Font.BOLD, new Color(0xD0, 0x3B, 0x3B))));
            }

            doc.add(spacer(14));
            doc.add(qrBlock(token));
            doc.add(spacer(14));
            doc.add(detailsTable(token));
            doc.add(spacer(16));
            doc.add(footer());

            doc.close();
        } catch (DocumentException ex) {
            throw new IOException("Could not build the token PDF", ex);
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------

    private static PdfPTable brandBand() throws DocumentException {
        PdfPTable band = new PdfPTable(1);
        band.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(NAVY);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(14);

        Paragraph name = new Paragraph("DigiQ", font(20, Font.BOLD, Color.WHITE));
        Paragraph tag = new Paragraph("Digital Queue Management System",
                font(9, Font.NORMAL, new Color(0xB7, 0xD3, 0xF6)));
        cell.addElement(name);
        cell.addElement(tag);
        band.addCell(cell);
        return band;
    }

    private static PdfPTable qrBlock(Token token) throws DocumentException, IOException {
        PdfPTable wrap = new PdfPTable(1);
        wrap.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(HAIRLINE);
        cell.setPadding(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Image qr = Image.getInstance(QrCodeService.png(token.getQrPayload(), 320));
        qr.scaleToFit(150, 150);
        qr.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(qr);

        Paragraph hint = centered("Present this code at the counter", font(8, Font.NORMAL, MUTED));
        hint.setSpacingBefore(8);
        cell.addElement(hint);

        wrap.addCell(cell);
        return wrap;
    }

    private static PdfPTable detailsTable(Token token) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.0f, 1.4f});

        row(table, "Customer", nullSafe(token.getCustomerName()));
        row(table, "Service", nullSafe(token.getServiceName()));
        row(table, "Issued", token.getIssuedAt() == null ? "-" : STAMP.format(token.getIssuedAt()));
        row(table, "Status", token.getStatus().getLabel());
        row(table, "Counter", token.getCounterName() == null ? "To be assigned" : token.getCounterName());

        if (token.getPositionInQueue() > 0) {
            row(table, "Position", "#" + token.getPositionInQueue() + " in queue");
            row(table, "Estimated wait", "~" + token.getEstimatedWaitMinutes() + " minutes");
        }
        row(table, "Reference", token.getQrPayload());
        return table;
    }

    private static void row(PdfPTable table, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label.toUpperCase(), font(8, Font.BOLD, MUTED)));
        l.setBorder(Rectangle.BOTTOM);
        l.setBorderColor(HAIRLINE);
        l.setPadding(7);

        PdfPCell v = new PdfPCell(new Phrase(value, font(10, Font.NORMAL, INK)));
        v.setBorder(Rectangle.BOTTOM);
        v.setBorderColor(HAIRLINE);
        v.setPadding(7);

        table.addCell(l);
        table.addCell(v);
    }

    private static Paragraph footer() {
        Paragraph p = centered(
                "Keep this ticket until your service is complete. "
                        + "Track your position live at the DigiQ portal.",
                font(8, Font.NORMAL, MUTED));
        p.setSpacingBefore(4);
        return p;
    }

    private static Paragraph centered(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private static Paragraph spacer(float height) {
        Paragraph p = new Paragraph(new Chunk(" "));
        p.setLeading(height);
        return p;
    }

    private static Font font(float size, int style, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private static String nullSafe(String value) {
        return value == null ? "-" : value;
    }
}
