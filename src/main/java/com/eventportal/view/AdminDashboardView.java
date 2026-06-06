package com.eventportal.view;

import com.eventportal.controller.DashboardController;
import com.eventportal.controller.EventController;
import com.eventportal.controller.RegistrationController;
import com.eventportal.model.CategoryStat;
import com.eventportal.model.DashboardSummary;
import com.eventportal.model.MonthlyRegistrationStat;
import com.eventportal.model.PopularEventStat;
import com.eventportal.model.User;
import com.eventportal.util.FxTaskRunner;
import com.eventportal.util.NavigationManager;
import com.eventportal.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AdminDashboardView {
    private final User user;
    private final DashboardController dashboardController;
    private final EventController eventController;
    private final RegistrationController registrationController;
    private final NavigationManager navigationManager;
    private final SessionManager sessionManager;
    private final Parent loginRoot;

    public AdminDashboardView(
            User user,
            DashboardController dashboardController,
            EventController eventController,
            RegistrationController registrationController,
            NavigationManager navigationManager,
            SessionManager sessionManager,
            Parent loginRoot
    ) {
        this.user = user;
        this.dashboardController = dashboardController;
        this.eventController = eventController;
        this.registrationController = registrationController;
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

        Button createEventButton = new Button("Create Event");
        createEventButton.getStyleClass().add("primary-action");
        createEventButton.setOnAction(event -> openEventManagement());

        Button manageEventsButton = new Button("Manage Events");
        manageEventsButton.getStyleClass().add("primary-action");
        manageEventsButton.setOnAction(event -> openEventManagement());

        Button reportsButton = new Button("View Reports");
        reportsButton.setOnAction(event -> navigationManager.show(new AdminReportView(dashboardController, navigationManager).build()));

        Parent body = dashboardBody();

        HBox actions = new HBox(12, createEventButton, manageEventsButton, reportsButton);

        VBox root = new VBox(20, header, actions, body);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(24));
        return root;
    }

    private void openEventManagement() {
        navigationManager.show(new AdminEventManagementView(
                eventController,
                registrationController,
                user,
                navigationManager
        ).build());
    }

    private Parent dashboardBody() {
        VBox container = new VBox(label("Loading dashboard...", "muted-text"));
        container.getStyleClass().add("content-panel-wide");
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(32));
        FxTaskRunner.run(
                "admin-dashboard-loader",
                dashboardController::loadAdminSummary,
                summary -> {
                    container.setAlignment(Pos.TOP_LEFT);
                    container.getChildren().setAll(dashboardContent(summary));
                },
                failure -> container.getChildren().setAll(label(
                        "Unable to load dashboard. Start PostgreSQL and try again.",
                        "error-text"
                ))
        );
        return container;
    }

    private static Parent dashboardContent(DashboardSummary summary) {
        VBox content = new VBox(
                18,
                statGrid(summary),
                categorySection(summary),
                monthlyRegistrationSection(summary),
                popularEventsSection(summary)
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        return scrollPane;
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

        PieChart chart = new PieChart();
        chart.setAnimated(false);
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setPrefHeight(300);
        for (CategoryStat stat : summary.categoryDistribution()) {
            if (stat.eventCount() > 0) {
                chart.getData().add(new PieChart.Data(stat.category().name(), stat.eventCount()));
            }
        }
        if (chart.getData().isEmpty()) {
            Label empty = new Label("No events created yet.");
            empty.getStyleClass().add("muted-text");
            box.getChildren().add(empty);
        } else {
            box.getChildren().add(chart);
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

    private static Parent monthlyRegistrationSection(DashboardSummary summary) {
        VBox box = new VBox(8);
        Label title = new Label("Registrations by Month");
        title.getStyleClass().add("section-title");
        box.getChildren().add(title);
        if (summary.monthlyRegistrations().isEmpty()) {
            Label empty = new Label("No registrations yet.");
            empty.getStyleClass().add("muted-text");
            box.getChildren().add(empty);
            return box;
        }

        CategoryAxis monthAxis = new CategoryAxis();
        monthAxis.setLabel("Month");
        NumberAxis countAxis = new NumberAxis();
        countAxis.setLabel("Registrations");
        countAxis.setForceZeroInRange(true);

        BarChart<String, Number> chart = new BarChart<>(monthAxis, countAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCategoryGap(18);
        chart.setPrefHeight(320);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Registrations");
        for (MonthlyRegistrationStat stat : summary.monthlyRegistrations()) {
            series.getData().add(new XYChart.Data<>(stat.month().toString(), stat.registrationCount()));
        }
        chart.getData().add(series);
        box.getChildren().add(chart);
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

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }
}
