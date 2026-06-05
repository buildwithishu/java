package com.eventportal.view;

import com.eventportal.controller.CertificateController;
import com.eventportal.model.Certificate;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

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
        VBox row = new VBox(6, title, path);
        row.getStyleClass().add("event-card");
        row.setStyle("-fx-padding: 14;");
        return row;
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }
}
