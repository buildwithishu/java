package com.eventportal.view;

import com.eventportal.controller.CertificateController;
import com.eventportal.model.Certificate;
import com.eventportal.util.DateTimeUtil;
import com.eventportal.util.FxTaskRunner;
import com.eventportal.util.PDFPreviewUtil;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

public class CertificateView {
    private final CertificateController certificateController;

    public CertificateView(CertificateController certificateController) {
        this.certificateController = certificateController;
    }

    public Parent build() {
        VBox list = new VBox(12);
        list.getChildren().add(label("Loading certificates...", "muted-text"));
        FxTaskRunner.run(
                "certificate-list-loader",
                certificateController::findMyCertificates,
                certificates -> renderCertificates(list, certificates),
                failure -> {
                    list.getChildren().clear();
                    list.getChildren().add(label(
                            "Unable to load certificates. Start PostgreSQL and try again.",
                            "error-text"
                    ));
                }
        );

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        return scrollPane;
    }

    private void renderCertificates(VBox list, List<Certificate> certificates) {
        list.getChildren().clear();
        if (certificates.isEmpty()) {
            list.getChildren().add(label("No certificates generated yet.", "muted-text"));
            return;
        }
        for (Certificate certificate : certificates) {
            list.getChildren().add(certificateRow(certificate));
        }
    }

    private Parent certificateRow(Certificate certificate) {
        Label title = label("Certificate #" + certificate.id(), "card-title");
        Label path = label(certificate.certificatePath(), "muted-text");
        Label generatedAt = label("Generated " + DateTimeUtil.formatDateTime(certificate.generatedAt()), "muted-text");
        Label message = label("", "error-text");

        Button openButton = new Button("Open PDF");
        openButton.setOnAction(event -> openCertificate(certificate, message));

        Button previewButton = new Button("Preview");
        previewButton.setOnAction(event -> previewCertificate(certificate, previewButton, message));

        Button saveButton = new Button("Save Copy");
        saveButton.setOnAction(event -> saveCertificateCopy(certificate, saveButton, message));

        VBox text = new VBox(4, title, generatedAt, path, message);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox actions = new HBox(8, previewButton, openButton, saveButton);
        HBox row = new HBox(12, text, actions);
        row.getStyleClass().add("event-card");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        return row;
    }

    private void previewCertificate(Certificate certificate, Button previewButton, Label message) {
        Path certificatePath = resolveCertificatePath(certificate, message);
        if (certificatePath == null) {
            return;
        }

        previewButton.setDisable(true);
        FxTaskRunner.run(
                "certificate-preview-renderer",
                () -> PDFPreviewUtil.renderFirstPage(certificatePath),
                renderedPage -> {
                    previewButton.setDisable(false);
                    showPreviewDialog(renderedPage);
                    showSuccess(message, "Certificate preview opened.");
                },
                failure -> {
                    previewButton.setDisable(false);
                    showError(message, "Unable to preview certificate PDF.");
                }
        );
    }

    private static void showPreviewDialog(BufferedImage renderedPage) {
        ImageView preview = new ImageView(SwingFXUtils.toFXImage(renderedPage, null));
        preview.setPreserveRatio(true);
        preview.setFitWidth(720);

        ScrollPane previewPane = new ScrollPane(preview);
        previewPane.setFitToWidth(true);
        previewPane.setPrefViewportWidth(740);
        previewPane.setPrefViewportHeight(620);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Certificate Preview");
        dialog.getDialogPane().setContent(previewPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void openCertificate(Certificate certificate, Label message) {
        Path certificatePath = resolveCertificatePath(certificate, message);
        if (certificatePath == null) {
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

    private void saveCertificateCopy(Certificate certificate, Button saveButton, Label message) {
        Path sourcePath = resolveCertificatePath(certificate, message);
        if (sourcePath == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Certificate PDF");
        fileChooser.setInitialFileName(sourcePath.getFileName().toString());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));

        java.io.File destinationFile = fileChooser.showSaveDialog(saveButton.getScene().getWindow());
        if (destinationFile == null) {
            return;
        }

        Path destinationPath = destinationFile.toPath().toAbsolutePath().normalize();
        if (sourcePath.equals(destinationPath)) {
            showSuccess(message, "Certificate is already saved at this location.");
            return;
        }

        try {
            Path copiedPath = certificateController.copyCertificate(certificate, destinationPath);
            showSuccess(message, "Certificate copy saved to " + copiedPath);
        } catch (IOException exception) {
            showError(message, "Unable to save certificate copy.");
        }
    }

    private Path resolveCertificatePath(Certificate certificate, Label message) {
        try {
            return certificateController.resolveCertificatePath(certificate);
        } catch (IOException exception) {
            showError(message, exception.getMessage());
            return null;
        } catch (InvalidPathException exception) {
            showError(message, "Certificate path is invalid.");
            return null;
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
