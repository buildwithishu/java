package com.eventportal.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class CertificateFileUtil {
    private CertificateFileUtil() {
    }

    public static Path resolveExisting(String storedPath, Path allowedDirectory) throws IOException {
        if (storedPath == null || storedPath.isBlank()) {
            throw new IllegalArgumentException("Certificate path is required");
        }
        Path allowedRoot = Objects.requireNonNull(allowedDirectory, "allowedDirectory")
                .toAbsolutePath()
                .normalize()
                .toRealPath();
        Path certificatePath = Path.of(storedPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(certificatePath)) {
            throw new IOException("Certificate file does not exist");
        }
        Path realCertificatePath = certificatePath.toRealPath();
        if (!realCertificatePath.startsWith(allowedRoot)) {
            throw new IOException("Certificate file is outside the configured certificate directory");
        }
        return realCertificatePath;
    }

    public static Path copyTo(String storedPath, Path allowedDirectory, Path destination) throws IOException {
        Path sourcePath = resolveExisting(storedPath, allowedDirectory);
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
