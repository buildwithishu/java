package com.eventportal.view;

import com.eventportal.controller.AuthController;
import com.eventportal.util.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

public class RegisterView {
    private final AuthController authController;
    private final NavigationManager navigationManager;

    public RegisterView(AuthController authController, NavigationManager navigationManager) {
        this.authController = authController;
        this.navigationManager = navigationManager;
    }

    public Parent build() {
        Label title = new Label("Student Registration");
        title.getStyleClass().add("screen-title");

        TextField usernameField = textField("Username");
        TextField emailField = textField("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(360);
        TextField fullNameField = textField("Full name");

        ComboBox<String> departmentBox = new ComboBox<>();
        departmentBox.getItems().addAll("Computer Science", "Electronics", "Mechanical", "Civil", "Management", "Other");
        departmentBox.setPromptText("Department");
        departmentBox.setMaxWidth(360);

        Label messageLabel = new Label();
        messageLabel.getStyleClass().add("error-text");

        Button registerButton = new Button("Register");
        registerButton.getStyleClass().add("primary-action");
        registerButton.setDefaultButton(true);
        registerButton.setOnAction(event -> register(
                usernameField,
                emailField,
                passwordField,
                fullNameField,
                departmentBox,
                messageLabel
        ));

        Hyperlink backLink = new Hyperlink("Back to login");
        backLink.setOnAction(event -> navigationManager.goBack());

        HBox actions = new HBox(12, registerButton, backLink);
        actions.setAlignment(Pos.CENTER);

        VBox form = new VBox(
                14,
                title,
                usernameField,
                emailField,
                passwordField,
                fullNameField,
                departmentBox,
                messageLabel,
                actions
        );
        form.getStyleClass().add("auth-panel");
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(32));

        VBox root = new VBox(form);
        root.getStyleClass().add("app-root");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(32));
        return root;
    }

    private void register(
            TextField usernameField,
            TextField emailField,
            PasswordField passwordField,
            TextField fullNameField,
            ComboBox<String> departmentBox,
            Label messageLabel
    ) {
        try {
            authController.registerStudent(
                    usernameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    fullNameField.getText(),
                    departmentBox.getValue()
            );
            messageLabel.getStyleClass().remove("error-text");
            if (!messageLabel.getStyleClass().contains("success-text")) {
                messageLabel.getStyleClass().add("success-text");
            }
            messageLabel.setText("Account created. You can now log in.");
        } catch (SQLException exception) {
            messageLabel.getStyleClass().remove("success-text");
            if (!messageLabel.getStyleClass().contains("error-text")) {
                messageLabel.getStyleClass().add("error-text");
            }
            messageLabel.setText("Database connection failed. Start PostgreSQL and try again.");
        } catch (IllegalArgumentException exception) {
            messageLabel.getStyleClass().remove("success-text");
            if (!messageLabel.getStyleClass().contains("error-text")) {
                messageLabel.getStyleClass().add("error-text");
            }
            messageLabel.setText(exception.getMessage());
        }
    }

    private static TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(360);
        return field;
    }
}
