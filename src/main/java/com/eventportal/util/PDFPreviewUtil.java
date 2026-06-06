package com.eventportal.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class PDFPreviewUtil {
    private static final float PREVIEW_DPI = 110;

    private PDFPreviewUtil() {
    }

    public static BufferedImage renderFirstPage(Path pdfPath) throws IOException {
        Path normalizedPath = Objects.requireNonNull(pdfPath, "pdfPath")
                .toAbsolutePath()
                .normalize();
        try (PDDocument document = Loader.loadPDF(normalizedPath.toFile())) {
            if (document.getNumberOfPages() == 0) {
                throw new IOException("Certificate PDF has no pages");
            }
            PDFRenderer renderer = new PDFRenderer(document);
            return renderer.renderImageWithDPI(0, PREVIEW_DPI, ImageType.RGB);
        }
    }
}
