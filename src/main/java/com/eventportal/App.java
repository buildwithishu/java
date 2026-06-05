package com.eventportal;

import atlantafx.base.theme.PrimerDark;
import com.eventportal.controller.AuthController;
import com.eventportal.controller.CertificateController;
import com.eventportal.controller.DashboardController;
import com.eventportal.controller.EventController;
import com.eventportal.controller.NotificationController;
import com.eventportal.controller.RegistrationController;
import com.eventportal.dao.CertificateDAO;
import com.eventportal.dao.DatabaseConnection;
import com.eventportal.dao.EventDAO;
import com.eventportal.dao.NotificationDAO;
import com.eventportal.dao.RegistrationDAO;
import com.eventportal.dao.UserDAO;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.AppConfig;
import com.eventportal.util.NavigationManager;
import com.eventportal.util.SessionManager;
import com.eventportal.view.DashboardPlaceholderView;
import com.eventportal.view.LoginView;
import com.eventportal.view.AdminDashboardView;
import com.eventportal.view.StudentDashboardView;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class App extends Application {
    public static final String APP_NAME = "College Event Management Portal";

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        primaryStage.setTitle(APP_NAME);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        NavigationManager navigationManager = new NavigationManager(primaryStage);
        SessionManager sessionManager = SessionManager.getInstance();
        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();
        UserDAO userDAO = new UserDAO(databaseConnection);
        EventDAO eventDAO = new EventDAO(databaseConnection);
        RegistrationDAO registrationDAO = new RegistrationDAO(databaseConnection);
        NotificationController notificationController = new NotificationController(new NotificationDAO(databaseConnection));
        CertificateController certificateController = new CertificateController(
                new CertificateDAO(databaseConnection),
                registrationDAO,
                eventDAO,
                userDAO,
                notificationController,
                sessionManager,
                AppConfig.load()
        );
        RegistrationController registrationController = new RegistrationController(
                registrationDAO,
                eventDAO,
                notificationController,
                sessionManager
        );
        EventController eventController = new EventController(
                eventDAO,
                sessionManager,
                registrationDAO,
                notificationController
        );
        DashboardController dashboardController = new DashboardController(eventDAO, registrationDAO, userDAO);
        AuthController authController = new AuthController(
                userDAO,
                sessionManager
        );

        Parent[] loginRootHolder = new Parent[1];
        LoginView loginView = new LoginView(authController, navigationManager, user -> showDashboard(
                navigationManager,
                sessionManager,
                eventController,
                registrationController,
                certificateController,
                notificationController,
                dashboardController,
                user,
                loginRootHolder[0]
        ));
        Parent loginRoot = loginView.build();
        loginRootHolder[0] = loginRoot;

        navigationManager.replace(loginRoot);
        primaryStage.show();
    }

    private void showDashboard(
            NavigationManager navigationManager,
            SessionManager sessionManager,
            EventController eventController,
            RegistrationController registrationController,
            CertificateController certificateController,
            NotificationController notificationController,
            DashboardController dashboardController,
            User user,
            Parent loginRoot
    ) {
        if (user.role() == Role.STUDENT) {
            navigationManager.replace(new StudentDashboardView(
                    user,
                    eventController,
                    registrationController,
                    certificateController,
                    notificationController,
                    navigationManager,
                    sessionManager,
                    loginRoot
            ).build());
            return;
        }
        if (user.role() == Role.ADMIN) {
            navigationManager.replace(new AdminDashboardView(
                    user,
                    dashboardController,
                    eventController,
                    navigationManager,
                    sessionManager,
                    loginRoot
            ).build());
            return;
        }
        navigationManager.replace(new DashboardPlaceholderView(user, navigationManager, sessionManager, loginRoot).build());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
