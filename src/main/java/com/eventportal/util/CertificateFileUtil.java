package com.eventportal.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class CertificateFileUtil {
    private CertificateFileUtil() {
    }

    public static Path resolveExisting(String storedPath) throws IOException {
        if (storedPath == null || storedPath.isBlank()) {
            throw new IllegalArgumentException("Certificate path is required");
        }
        Path certificatePath = Path.of(storedPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(certificatePath)) {
            throw new IOException("Certificate file does not exist: " + certificatePath);
        }
        return certificatePath;
    }

    public static Path copyTo(String storedPath, Path destination) throws IOException {
        Path sourcePath = resolveExisting(storedPath);
        Path destinationPath = Objects.requireNonNull(destination, "destination")
                .toAbsolutePath()
                .normalize();
        if (sourcePath.equals(destinationPath)) {
            return destinationPath;
        }
        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        return destinationPath;
    }
}
