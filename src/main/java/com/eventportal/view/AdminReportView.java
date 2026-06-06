package com.eventportal.view;

import com.eventportal.controller.DashboardController;
import com.eventportal.model.CategoryStat;
import com.eventportal.model.DashboardSummary;
import com.eventportal.model.MonthlyRegistrationStat;
import com.eventportal.model.PopularEventStat;
import com.eventportal.util.FxTaskRunner;
import com.eventportal.util.NavigationManager;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AdminReportView {
    private final DashboardController dashboardController;
    private final NavigationManager navigationManager;

    public AdminReportView(DashboardController dashboardController, NavigationManager navigationManager) {
        this.dashboardController = dashboardController;
        this.navigationManager = navigationManager;
    }

    public Parent build() {
        Button backButton = new Button("Back");
        backButton.setOnAction(event -> navigationManager.goBack());

        Label title = new Label("Reports");
        title.getStyleClass().add("screen-title");

        TextArea reportArea = new TextArea("Loading report...");
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setPrefRowCount(24);
        FxTaskRunner.run(
                "admin-report-loader",
                dashboardController::loadAdminSummary,
                summary -> reportArea.setText(buildReportText(summary)),
                failure -> reportArea.setText("Unable to load report. Start PostgreSQL and try again.")
        );

        VBox root = new VBox(16, new HBox(12, backButton, title), reportArea);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(24));
        return root;
    }

    private static String buildReportText(DashboardSummary summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("College Event Management Portal Report\n\n");
        builder.append("Total events: ").append(summary.totalEvents()).append('\n');
        builder.append("Total registrations: ").append(summary.totalRegistrations()).append('\n');
        builder.append("Active students: ").append(summary.activeStudents()).append("\n\n");
        builder.append("Events by category\n");
        for (CategoryStat stat : summary.categoryDistribution()) {
            builder.append("- ").append(stat.category()).append(": ").append(stat.eventCount()).append('\n');
        }
        builder.append("\nRegistrations by month\n");
        for (MonthlyRegistrationStat stat : summary.monthlyRegistrations()) {
            builder.append("- ").append(stat.month()).append(": ").append(stat.registrationCount()).append('\n');
        }
        builder.append("\nPopular events\n");
        for (PopularEventStat event : summary.popularEvents()) {
            builder.append("- ")
                    .append(event.title())
                    .append(": ")
                    .append(event.registeredCount())
                    .append(" / ")
                    .append(event.capacity())
                    .append('\n');
        }
        return builder.toString();
    }
}
