package com.eventportal;

import atlantafx.base.theme.PrimerDark;
import com.eventportal.controller.AuthController;
import com.eventportal.dao.DatabaseConnection;
import com.eventportal.dao.UserDAO;
import com.eventportal.model.User;
import com.eventportal.util.NavigationManager;
import com.eventportal.util.SessionManager;
import com.eventportal.view.DashboardPlaceholderView;
import com.eventportal.view.LoginView;
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
        AuthController authController = new AuthController(
                new UserDAO(DatabaseConnection.getInstance()),
                sessionManager
        );

        Parent[] loginRootHolder = new Parent[1];
        LoginView loginView = new LoginView(authController, navigationManager, user -> showDashboard(
                navigationManager,
                sessionManager,
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
            User user,
            Parent loginRoot
    ) {
        navigationManager.replace(new DashboardPlaceholderView(user, navigationManager, sessionManager, loginRoot).build());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
