package com.eventportal.view;

import com.eventportal.controller.CertificateController;
import com.eventportal.controller.EventController;
import com.eventportal.controller.NotificationController;
import com.eventportal.controller.RegistrationController;
import com.eventportal.model.RegistrationDetail;
import com.eventportal.model.User;
import com.eventportal.util.FxTaskRunner;
import com.eventportal.util.NavigationManager;
import com.eventportal.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class StudentDashboardView {
    private final User user;
    private final EventController eventController;
    private final RegistrationController registrationController;
    private final CertificateController certificateController;
    private final NotificationController notificationController;
    private final NavigationManager navigationManager;
    private final SessionManager sessionManager;
    private final Parent loginRoot;
    private BorderPane root;

    public StudentDashboardView(
            User user,
            EventController eventController,
            RegistrationController registrationController,
            CertificateController certificateController,
            NotificationController notificationController,
            NavigationManager navigationManager,
            SessionManager sessionManager,
            Parent loginRoot
    ) {
        this.user = user;
        this.eventController = eventController;
        this.registrationController = registrationController;
        this.certificateController = certificateController;
        this.notificationController = notificationController;
        this.navigationManager = navigationManager;
        this.sessionManager = sessionManager;
        this.loginRoot = loginRoot;
    }

    public Parent build() {
        root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(24));

        root.setTop(header());
        root.setLeft(sidebar());
        showUpcomingEvents();
        return root;
    }

    private Parent header() {
        Label title = new Label("Student Dashboard");
        title.getStyleClass().add("screen-title");

        Label welcome = new Label("Welcome, " + user.fullName());
        welcome.getStyleClass().add("muted-text");

        VBox text = new VBox(4, title, welcome, quickStats());
        HBox header = new HBox(text);
        header.setPadding(new Insets(0, 0, 18, 0));
        return header;
    }

    private Parent quickStats() {
        VBox container = new VBox(label("Loading quick stats...", "muted-text"));
        container.setPadding(new Insets(10, 0, 0, 0));
        FxTaskRunner.run(
                "student-stats-loader",
                this::loadQuickStats,
                stats -> container.getChildren().setAll(quickStatsRow(stats)),
                failure -> container.getChildren().setAll(label(
                        "Quick stats unavailable until PostgreSQL is running.",
                        "error-text"
                ))
        );
        return container;
    }

    private StudentStats loadQuickStats() throws Exception {
        List<RegistrationDetail> registrations = registrationController.findMyRegistrationDetails();
        long activeRegistrations = registrations.stream()
                .filter(RegistrationDetail::isActive)
                .count();
        int certificateCount = certificateController.findMyCertificates().size();
        int unreadNotifications = notificationController.countUnread(user.id());
        return new StudentStats(activeRegistrations, certificateCount, unreadNotifications);
    }

    private static Parent quickStatsRow(StudentStats stats) {
        return new HBox(
                12,
                statCard("Registered Events", stats.activeRegistrations()),
                statCard("Certificates", stats.certificateCount()),
                statCard("Unread Notifications", stats.unreadNotifications())
        );
    }

    private Parent sidebar() {
        Button browseButton = new Button("Browse Events");
        browseButton.setMaxWidth(Double.MAX_VALUE);
        browseButton.setOnAction(event -> showUpcomingEvents());

        Button registrationsButton = new Button("My Registrations");
        registrationsButton.setMaxWidth(Double.MAX_VALUE);
        registrationsButton.setOnAction(event -> setContent("My Registrations", new MyRegistrationsView(
                registrationController,
                certificateController
        ).build()));

        Button certificatesButton = new Button("Certificates");
        certificatesButton.setMaxWidth(Double.MAX_VALUE);
        certificatesButton.setOnAction(event -> setContent("Certificates", new CertificateView(certificateController).build()));

        Button notificationsButton = new Button("Notifications");
        notificationsButton.setMaxWidth(Double.MAX_VALUE);
        notificationsButton.setOnAction(event -> setContent("Notifications", new NotificationView(notificationController, user).build()));

        Button profileButton = new Button("Profile");
        profileButton.setMaxWidth(Double.MAX_VALUE);
        profileButton.setOnAction(event -> setContent("Profile", new ProfileView(user).build()));

        Button logoutButton = new Button("Logout");
        logoutButton.setMaxWidth(Double.MAX_VALUE);
        logoutButton.setOnAction(event -> {
            sessionManager.logout();
            navigationManager.replace(loginRoot);
        });

        VBox sidebar = new VBox(12, browseButton, registrationsButton, certificatesButton, notificationsButton, profileButton, logoutButton);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(0, 18, 0, 0));
        sidebar.setPrefWidth(180);
        return sidebar;
    }

    private void showUpcomingEvents() {
        VBox eventList = new VBox(label("Loading upcoming events...", "muted-text"));
        eventList.setAlignment(Pos.CENTER);
        setContent("Upcoming Events", eventList);
        FxTaskRunner.run(
                "upcoming-event-loader",
                eventController::findUpcomingEvents,
                upcomingEvents -> {
                    eventList.setAlignment(Pos.TOP_LEFT);
                    eventList.getChildren().setAll(new EventListView(
                            registrationController,
                            eventController,
                            navigationManager,
                            user,
                            upcomingEvents
                    ).build());
                },
                failure -> eventList.getChildren().setAll(label(
                        "Unable to load events. Start PostgreSQL and try again.",
                        "error-text"
                ))
        );
    }

    private void setContent(String title, Parent body) {
        Label sectionTitle = new Label(title);
        sectionTitle.getStyleClass().add("section-title");

        VBox content = new VBox(14, sectionTitle, body);
        content.getStyleClass().add("content-panel-wide");
        content.setPadding(new Insets(20));
        root.setCenter(content);
    }

    private static Parent statCard(String label, long value) {
        Label valueLabel = new Label(String.valueOf(value));
        valueLabel.getStyleClass().add("stat-value");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("muted-text");

        VBox card = new VBox(4, valueLabel, nameLabel);
        card.getStyleClass().add("event-card");
        card.setPadding(new Insets(12));
        card.setMinWidth(150);
        return card;
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private record StudentStats(long activeRegistrations, int certificateCount, int unreadNotifications) {
    }
}
