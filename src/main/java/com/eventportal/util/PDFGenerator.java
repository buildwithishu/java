package com.eventportal.util;

import com.eventportal.model.Event;
import com.eventportal.model.Registration;
import com.eventportal.model.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PDFGenerator {
    private static final DateTimeFormatter CERTIFICATE_DATE = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final PDType1Font TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font ACCENT_FONT = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);

    private PDFGenerator() {
    }

    public static Path generateCertificate(User user, Event event, Registration registration, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        Path safeOutputDirectory = outputDirectory.toAbsolutePath().normalize();
        String fileName = sanitizeFileName(registration.uniqueRegId() + "-" + user.fullName()) + ".pdf";
        Path outputPath = safeOutputDirectory.resolve(fileName).normalize();

        if (!outputPath.startsWith(safeOutputDirectory)) {
            throw new IOException("Certificate output path escaped the configured directory");
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawCenteredText(content, "CERTIFICATE OF PARTICIPATION", TITLE_FONT, 22, 297, 700);
                drawCenteredText(content, "College Event Management Portal", BODY_FONT, 13, 297, 665);
                drawCenteredText(content, "This certificate is proudly presented to", BODY_FONT, 14, 297, 600);
                drawCenteredText(content, user.fullName(), ACCENT_FONT, 28, 297, 555);
                drawCenteredText(content, "for participating in", BODY_FONT, 14, 297, 510);
                drawCenteredText(content, event.title(), TITLE_FONT, 20, 297, 470);
                drawCenteredText(content, "held on " + event.eventDate().format(CERTIFICATE_DATE), BODY_FONT, 13, 297, 430);
                drawCenteredText(content, "Certificate No: " + registration.uniqueRegId(), BODY_FONT, 11, 297, 370);
                drawCenteredText(content, "Generated on " + LocalDate.now().format(CERTIFICATE_DATE), BODY_FONT, 11, 297, 345);

                content.moveTo(130, 250);
                content.lineTo(260, 250);
                content.moveTo(335, 250);
                content.lineTo(465, 250);
                content.stroke();

                drawCenteredText(content, "Event Coordinator", BODY_FONT, 11, 195, 230);
                drawCenteredText(content, "Principal", BODY_FONT, 11, 400, 230);
            }

            document.save(outputPath.toFile());
        }

        return outputPath;
    }

    private static void drawCenteredText(
            PDPageContentStream content,
            String text,
            PDType1Font font,
            int fontSize,
            float centerX,
            float y
    ) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(centerX - (textWidth / 2), y);
        content.showText(text);
        content.endText();
    }

    private static String sanitizeFileName(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9._-]+", "-").replaceAll("(^-+|-+$)", "");
    }
}
