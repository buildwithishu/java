package com.eventportal.view;

import com.eventportal.controller.DashboardController;
import com.eventportal.model.CategoryStat;
import com.eventportal.model.DashboardSummary;
import com.eventportal.model.PopularEventStat;
import com.eventportal.model.User;
import com.eventportal.util.NavigationManager;
import com.eventportal.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

public class AdminDashboardView {
    private final User user;
    private final DashboardController dashboardController;
    private final NavigationManager navigationManager;
    private final SessionManager sessionManager;
    private final Parent loginRoot;

    public AdminDashboardView(
            User user,
            DashboardController dashboardController,
            NavigationManager navigationManager,
            SessionManager sessionManager,
            Parent loginRoot
    ) {
        this.user = user;
        this.dashboardController = dashboardController;
        this.navigationManager = navigationManager;
        this.sessionManager = sessionManager;
        this.loginRoot = loginRoot;
    }

    public Parent build() {
        Label title = new Label("Admin Dashboard");
        title.getStyleClass().add("screen-title");

        Label welcome = new Label("Welcome, " + user.fullName());
        welcome.getStyleClass().add("muted-text");

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> {
            sessionManager.logout();
            navigationManager.replace(loginRoot);
        });

        HBox header = new HBox(16, new VBox(4, title, welcome), logoutButton);
        header.setAlignment(Pos.CENTER_LEFT);

        Parent body = dashboardBody();

        VBox root = new VBox(20, header, body);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(24));
        return root;
    }

    private Parent dashboardBody() {
        try {
            DashboardSummary summary = dashboardController.loadAdminSummary();
            VBox content = new VBox(18, statGrid(summary), categorySection(summary), popularEventsSection(summary));
            content.getStyleClass().add("content-panel-wide");
            content.setPadding(new Insets(20));
            return content;
        } catch (SQLException exception) {
            Label error = new Label("Unable to load dashboard. Start PostgreSQL and try again.");
            error.getStyleClass().add("error-text");
            VBox errorBox = new VBox(error);
            errorBox.getStyleClass().add("content-panel-wide");
            errorBox.setAlignment(Pos.CENTER);
            errorBox.setPadding(new Insets(32));
            return errorBox;
        }
    }

    private static Parent statGrid(DashboardSummary summary) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.add(statCard("Total Events", summary.totalEvents()), 0, 0);
        grid.add(statCard("Registrations", summary.totalRegistrations()), 1, 0);
        grid.add(statCard("Students", summary.activeStudents()), 2, 0);
        return grid;
    }

    private static Parent categorySection(DashboardSummary summary) {
        VBox box = new VBox(8);
        Label title = new Label("Events by Category");
        title.getStyleClass().add("section-title");
        box.getChildren().add(title);
        for (CategoryStat stat : summary.categoryDistribution()) {
            Label row = new Label(stat.category().name() + ": " + stat.eventCount());
            row.getStyleClass().add("muted-text");
            box.getChildren().add(row);
        }
        return box;
    }

    private static Parent popularEventsSection(DashboardSummary summary) {
        VBox box = new VBox(8);
        Label title = new Label("Popular Events");
        title.getStyleClass().add("section-title");
        box.getChildren().add(title);
        if (summary.popularEvents().isEmpty()) {
            Label empty = new Label("No events created yet.");
            empty.getStyleClass().add("muted-text");
            box.getChildren().add(empty);
            return box;
        }
        for (PopularEventStat event : summary.popularEvents()) {
            Label row = new Label(event.title() + " — " + event.registeredCount() + " / " + event.capacity());
            row.getStyleClass().add("muted-text");
            box.getChildren().add(row);
        }
        return box;
    }

    private static Parent statCard(String label, int value) {
        Label valueLabel = new Label(String.valueOf(value));
        valueLabel.getStyleClass().add("stat-value");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("muted-text");

        VBox card = new VBox(6, valueLabel, nameLabel);
        card.getStyleClass().add("event-card");
        card.setPadding(new Insets(18));
        card.setMinWidth(180);
        return card;
    }
}
