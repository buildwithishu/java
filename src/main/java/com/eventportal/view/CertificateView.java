package com.eventportal.view;

import com.eventportal.controller.CertificateController;
import com.eventportal.model.Certificate;
import com.eventportal.util.DateTimeUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

public class CertificateView {
    private final CertificateController certificateController;

    public CertificateView(CertificateController certificateController) {
        this.certificateController = certificateController;
    }

    public Parent build() {
        VBox list = new VBox(12);
        try {
            List<Certificate> certificates = certificateController.findMyCertificates();
            if (certificates.isEmpty()) {
                list.getChildren().add(label("No certificates generated yet.", "muted-text"));
            } else {
                for (Certificate certificate : certificates) {
                    list.getChildren().add(certificateRow(certificate));
                }
            }
        } catch (SQLException exception) {
            list.getChildren().add(label("Unable to load certificates. Start PostgreSQL and try again.", "error-text"));
        }

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        return scrollPane;
    }

    private static Parent certificateRow(Certificate certificate) {
        Label title = label("Certificate #" + certificate.id(), "card-title");
        Label path = label(certificate.certificatePath(), "muted-text");
        Label generatedAt = label("Generated " + DateTimeUtil.formatDateTime(certificate.generatedAt()), "muted-text");
        Label message = label("", "error-text");

        Button openButton = new Button("Open PDF");
        openButton.setOnAction(event -> openCertificate(certificate, message));

        VBox text = new VBox(4, title, generatedAt, path, message);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(12, text, openButton);
        row.getStyleClass().add("event-card");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        return row;
    }

    private static void openCertificate(Certificate certificate, Label message) {
        Path certificatePath = Path.of(certificate.certificatePath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(certificatePath)) {
            showError(message, "Certificate file is missing: " + certificatePath);
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            showError(message, "Opening PDFs is not supported in this desktop environment.");
            return;
        }

        try {
            Desktop.getDesktop().open(certificatePath.toFile());
            showSuccess(message, "Opened certificate PDF.");
        } catch (IOException exception) {
            showError(message, "Unable to open certificate PDF.");
        }
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private static void showSuccess(Label label, String text) {
        label.getStyleClass().remove("error-text");
        if (!label.getStyleClass().contains("success-text")) {
            label.getStyleClass().add("success-text");
        }
        label.setText(text);
    }

    private static void showError(Label label, String text) {
        label.getStyleClass().remove("success-text");
        if (!label.getStyleClass().contains("error-text")) {
            label.getStyleClass().add("error-text");
        }
        label.setText(text);
    }
}
