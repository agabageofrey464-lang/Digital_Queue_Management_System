package com.digiq.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/** Renders a token's UUID as a PNG QR code. */
public final class QrCodeService {

    /** Ink colour of the QR modules - the brand navy, so the printed ticket matches the UI. */
    private static final int FOREGROUND = 0xFF0D366B;
    private static final int BACKGROUND = 0xFFFFFFFF;

    private QrCodeService() {
    }

    public static byte[] png(String content, int size) throws IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        // High correction: a counter ticket gets folded and smudged before it is scanned.
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out,
                    new MatrixToImageConfig(FOREGROUND, BACKGROUND));
            return out.toByteArray();
        } catch (WriterException ex) {
            throw new IOException("Could not encode QR code", ex);
        }
    }
}
