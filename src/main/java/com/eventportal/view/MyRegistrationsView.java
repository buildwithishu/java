package com.eventportal.view;

import com.eventportal.controller.CertificateController;
import com.eventportal.controller.RegistrationController;
import com.eventportal.model.RegistrationDetail;
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class MyRegistrationsView {
    private final RegistrationController registrationController;
    private final CertificateController certificateController;

    public MyRegistrationsView(RegistrationController registrationController, CertificateController certificateController) {
        this.registrationController = registrationController;
        this.certificateController = certificateController;
    }

    public Parent build() {
        VBox list = new VBox(12);
        refresh(list);

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        return scrollPane;
    }

    private void refresh(VBox list) {
        list.getChildren().clear();
        try {
            List<RegistrationDetail> registrations = registrationController.findMyRegistrationDetails();
            if (registrations.isEmpty()) {
                list.getChildren().add(emptyLabel("You have not registered for any events yet."));
                return;
            }
            for (RegistrationDetail registration : registrations) {
                list.getChildren().add(registrationCard(registration, list));
            }
        } catch (SQLException exception) {
            list.getChildren().add(errorLabel("Unable to load registrations. Start PostgreSQL and try again."));
        }
    }

    private Parent registrationCard(RegistrationDetail registration, VBox list) {
        Label title = new Label(registration.eventTitle());
        title.getStyleClass().add("card-title");

        Label details = new Label(DateTimeUtil.formatDate(registration.eventDate()) + " • " + registration.venue() + " • " + registration.uniqueRegId() + " • " + registration.status());
        details.getStyleClass().add("muted-text");

        Label message = new Label();
        message.getStyleClass().add("error-text");

        Button cancelButton = new Button("Cancel");
        cancelButton.setDisable(!registration.isActive());
        cancelButton.setOnAction(event -> cancelRegistration(registration.registrationId(), list, message));

        Button certificateButton = new Button("Generate Certificate");
        certificateButton.setDisable(!registration.isActive());
        certificateButton.setOnAction(event -> generateCertificate(registration.registrationId(), message));

        VBox text = new VBox(4, title, details, message);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(12, text, certificateButton, cancelButton);
        row.getStyleClass().add("event-card");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        return row;
    }

    private void cancelRegistration(long registrationId, VBox list, Label message) {
        try {
            if (registrationController.cancelRegistration(registrationId)) {
                refresh(list);
            }
        } catch (SQLException exception) {
            showError(message, "Unable to cancel registration. Database unavailable.");
        } catch (RuntimeException exception) {
            showError(message, exception.getMessage());
        }
    }

    private void generateCertificate(long registrationId, Label message) {
        try {
            certificateController.generateForRegistration(registrationId);
            showSuccess(message, "Certificate generated.");
        } catch (SQLException exception) {
            showError(message, "Unable to save certificate. Database unavailable.");
        } catch (IOException exception) {
            showError(message, "Unable to write certificate PDF.");
        } catch (RuntimeException exception) {
            showError(message, exception.getMessage());
        }
    }

    private static Label emptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted-text");
        return label;
    }

    private static Label errorLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("error-text");
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
