package com.eventportal.view;

import com.eventportal.model.User;
import com.eventportal.util.NavigationManager;
import com.eventportal.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardPlaceholderView {
    private final User user;
    private final NavigationManager navigationManager;
    private final SessionManager sessionManager;
    private final Parent loginRoot;

    public DashboardPlaceholderView(
            User user,
            NavigationManager navigationManager,
            SessionManager sessionManager,
            Parent loginRoot
    ) {
        this.user = user;
        this.navigationManager = navigationManager;
        this.sessionManager = sessionManager;
        this.loginRoot = loginRoot;
    }

    public Parent build() {
        Label title = new Label(LoginView.dashboardLabelFor(user));
        title.getStyleClass().add("screen-title");

        Label welcome = new Label("Welcome, " + user.fullName() + " (" + user.role() + ")");
        welcome.getStyleClass().add("muted-text");

        Label next = new Label("Event browsing, registrations, certificates, analytics, and notifications will attach here next.");
        next.getStyleClass().add("muted-text");

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> {
            sessionManager.logout();
            navigationManager.replace(loginRoot);
        });

        VBox content = new VBox(16, title, welcome, next, logoutButton);
        content.getStyleClass().add("content-panel");
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(32));

        VBox root = new VBox(content);
        root.getStyleClass().add("app-root");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(32));
        return root;
    }
}
