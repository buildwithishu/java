package com.eventportal;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
    public static final String APP_NAME = "College Event Management Portal";

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Label title = new Label(APP_NAME);
        title.getStyleClass().add("title-2");

        Label subtitle = new Label("JavaFX + PostgreSQL event management");
        Button loginButton = new Button("Login");
        loginButton.setDefaultButton(true);

        VBox root = new VBox(16, title, subtitle, loginButton);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(32));

        primaryStage.setTitle(APP_NAME);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
