package com.eventportal.view;

import com.eventportal.controller.AuthController;
import com.eventportal.model.User;
import com.eventportal.util.FxTaskRunner;
import com.eventportal.util.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class LoginView {
    private final AuthController authController;
    private final NavigationManager navigationManager;
    private final Consumer<User> loginSuccessHandler;

    public LoginView(AuthController authController, NavigationManager navigationManager, Consumer<User> loginSuccessHandler) {
        this.authController = authController;
        this.navigationManager = navigationManager;
        this.loginSuccessHandler = loginSuccessHandler;
    }

    public Parent build() {
        Label title = new Label("College Event Management Portal");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Sign in to manage events, registrations, certificates, and notifications.");
        subtitle.getStyleClass().add("muted-text");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(360);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(360);

        Label messageLabel = new Label();
        messageLabel.getStyleClass().add("error-text");

        Button loginButton = new Button("Login");
        loginButton.setDefaultButton(true);
        loginButton.getStyleClass().add("primary-action");

        Hyperlink registerLink = new Hyperlink("Create student account");
        registerLink.setOnAction(event -> navigationManager.show(new RegisterView(authController, navigationManager).build()));

        loginButton.setOnAction(event -> login(
                usernameField,
                passwordField,
                messageLabel,
                loginButton,
                registerLink
        ));

        HBox actions = new HBox(12, loginButton, registerLink);
        actions.setAlignment(Pos.CENTER);

        VBox form = new VBox(14, title, subtitle, usernameField, passwordField, messageLabel, actions);
        form.getStyleClass().add("auth-panel");
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(32));

        VBox root = new VBox(form);
        root.getStyleClass().add("app-root");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(32));
        return root;
    }

    private void login(
            TextField usernameField,
            PasswordField passwordField,
            Label messageLabel,
            Button loginButton,
            Hyperlink registerLink
    ) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        setBusy(loginButton, registerLink, true);
        messageLabel.setText("Signing in...");

        FxTaskRunner.run(
                "login-authenticator",
                () -> authController.login(username, password),
                user -> {
                    setBusy(loginButton, registerLink, false);
                    loginSuccessHandler.accept(user);
                },
                failure -> {
                    setBusy(loginButton, registerLink, false);
                    messageLabel.setText(operationMessage(
                            failure,
                            "Database connection failed. Start PostgreSQL and try again."
                    ));
                }
        );
    }

    private static void setBusy(Button loginButton, Hyperlink registerLink, boolean busy) {
        loginButton.setDisable(busy);
        loginButton.setText(busy ? "Signing in..." : "Login");
        registerLink.setDisable(busy);
    }

    private static String operationMessage(Throwable failure, String fallback) {
        if (failure instanceof IllegalArgumentException || failure instanceof IllegalStateException) {
            return failure.getMessage();
        }
        return fallback;
    }
}
