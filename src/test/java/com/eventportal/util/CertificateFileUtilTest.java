package com.eventportal.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertificateFileUtilTest {
    @TempDir
    Path tempDirectory;

    @Test
    void copyTo_shouldCopyCertificateAndReplaceExistingFile() throws IOException {
        Path source = tempDirectory.resolve("source.pdf");
        Path destination = tempDirectory.resolve("download.pdf");
        Files.writeString(source, "certificate");
        Files.writeString(destination, "old");

        Path copiedPath = CertificateFileUtil.copyTo(source.toString(), destination);

        assertThat(copiedPath).isEqualTo(destination.toAbsolutePath().normalize());
        assertThat(Files.readString(destination)).isEqualTo("certificate");
    }

    @Test
    void resolveExisting_shouldRejectMissingFile() {
        Path missing = tempDirectory.resolve("missing.pdf");

        assertThatThrownBy(() -> CertificateFileUtil.resolveExisting(missing.toString()))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("does not exist");
    }
}
