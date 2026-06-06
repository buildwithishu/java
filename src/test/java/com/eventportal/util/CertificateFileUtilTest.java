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
        Path certificateDirectory = Files.createDirectory(tempDirectory.resolve("certificates"));
        Path source = certificateDirectory.resolve("source.pdf");
        Path destination = tempDirectory.resolve("download.pdf");
        Files.writeString(source, "certificate");
        Files.writeString(destination, "old");

        Path copiedPath = CertificateFileUtil.copyTo(source.toString(), certificateDirectory, destination);

        assertThat(copiedPath).isEqualTo(destination.toAbsolutePath().normalize());
        assertThat(Files.readString(destination)).isEqualTo("certificate");
    }

    @Test
    void resolveExisting_shouldRejectMissingFile() {
        Path certificateDirectory = tempDirectory.resolve("certificates");
        Path missing = tempDirectory.resolve("missing.pdf");

        assertThatThrownBy(() -> CertificateFileUtil.resolveExisting(missing.toString(), certificateDirectory))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("certificates");
    }

    @Test
    void resolveExisting_shouldRejectFilesOutsideConfiguredDirectory() throws IOException {
        Path certificateDirectory = Files.createDirectory(tempDirectory.resolve("certificates"));
        Path outside = tempDirectory.resolve("outside.pdf");
        Files.writeString(outside, "not allowed");

        assertThatThrownBy(() -> CertificateFileUtil.resolveExisting(
                outside.toString(),
                certificateDirectory
        )).isInstanceOf(IOException.class)
                .hasMessageContaining("outside");
    }

    @Test
    void resolveExisting_shouldRejectSymlinkEscapes() throws IOException {
        Path certificateDirectory = Files.createDirectory(tempDirectory.resolve("certificates"));
        Path outside = tempDirectory.resolve("outside.pdf");
        Path link = certificateDirectory.resolve("linked.pdf");
        Files.writeString(outside, "not allowed");
        Files.createSymbolicLink(link, outside);

        assertThatThrownBy(() -> CertificateFileUtil.resolveExisting(
                link.toString(),
                certificateDirectory
        )).isInstanceOf(IOException.class)
                .hasMessageContaining("outside");
    }
}
