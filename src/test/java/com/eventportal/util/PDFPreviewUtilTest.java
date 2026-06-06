package com.eventportal.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PDFPreviewUtilTest {
    @TempDir
    private Path tempDirectory;

    @Test
    void renderFirstPage_shouldCreatePreviewImage() throws IOException {
        Path pdfPath = tempDirectory.resolve("certificate.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdfPath.toFile());
        }

        BufferedImage preview = PDFPreviewUtil.renderFirstPage(pdfPath);

        assertThat(preview.getWidth()).isGreaterThan(0);
        assertThat(preview.getHeight()).isGreaterThan(preview.getWidth());
    }

    @Test
    void renderFirstPage_shouldRejectPdfWithoutPages() throws IOException {
        Path pdfPath = tempDirectory.resolve("empty.pdf");
        try (PDDocument document = new PDDocument()) {
            document.save(pdfPath.toFile());
        }

        assertThatThrownBy(() -> PDFPreviewUtil.renderFirstPage(pdfPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("no pages");
    }
}
