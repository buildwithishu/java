package com.eventportal.view;

import com.eventportal.controller.EventController;
import com.eventportal.controller.RegistrationController;
import com.eventportal.model.Event;
import com.eventportal.model.User;
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

import java.sql.SQLException;
import java.util.List;

public class StudentDashboardView {
    private final User user;
    private final EventController eventController;
    private final RegistrationController registrationController;
    private final NavigationManager navigationManager;
    private final SessionManager sessionManager;
    private final Parent loginRoot;

    public StudentDashboardView(
            User user,
            EventController eventController,
            RegistrationController registrationController,
            NavigationManager navigationManager,
            SessionManager sessionManager,
            Parent loginRoot
    ) {
        this.user = user;
        this.eventController = eventController;
        this.registrationController = registrationController;
        this.navigationManager = navigationManager;
        this.sessionManager = sessionManager;
        this.loginRoot = loginRoot;
    }

    public Parent build() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(24));

        root.setTop(header());
        root.setLeft(sidebar());
        root.setCenter(content());
        return root;
    }

    private Parent header() {
        Label title = new Label("Student Dashboard");
        title.getStyleClass().add("screen-title");

        Label welcome = new Label("Welcome, " + user.fullName());
        welcome.getStyleClass().add("muted-text");

        VBox text = new VBox(4, title, welcome);
        HBox header = new HBox(text);
        header.setPadding(new Insets(0, 0, 18, 0));
        return header;
    }

    private Parent sidebar() {
        Button browseButton = new Button("Browse Events");
        browseButton.setMaxWidth(Double.MAX_VALUE);

        Button logoutButton = new Button("Logout");
        logoutButton.setMaxWidth(Double.MAX_VALUE);
        logoutButton.setOnAction(event -> {
            sessionManager.logout();
            navigationManager.replace(loginRoot);
        });

        VBox sidebar = new VBox(12, browseButton, logoutButton);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(0, 18, 0, 0));
        sidebar.setPrefWidth(180);
        return sidebar;
    }

    private Parent content() {
        Label sectionTitle = new Label("Upcoming Events");
        sectionTitle.getStyleClass().add("section-title");

        Parent eventList;
        try {
            List<Event> upcomingEvents = eventController.findUpcomingEvents();
            eventList = new EventListView(registrationController, upcomingEvents).build();
        } catch (SQLException exception) {
            Label error = new Label("Unable to load events. Start PostgreSQL and try again.");
            error.getStyleClass().add("error-text");
            VBox errorBox = new VBox(error);
            errorBox.setAlignment(Pos.CENTER);
            eventList = errorBox;
        }

        VBox content = new VBox(14, sectionTitle, eventList);
        content.getStyleClass().add("content-panel-wide");
        content.setPadding(new Insets(20));
        return content;
    }
}
