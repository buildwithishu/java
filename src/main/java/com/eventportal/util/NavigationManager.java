package com.eventportal.util;

import javafx.animation.FadeTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

public final class NavigationManager {
    private static final double DEFAULT_WIDTH = 1000;
    private static final double DEFAULT_HEIGHT = 680;

    private final Stage stage;
    private final Deque<Parent> backStack = new ArrayDeque<>();

    public NavigationManager(Stage stage) {
        this.stage = stage;
    }

    public void show(Parent root) {
        show(root, true);
    }

    public void replace(Parent root) {
        show(root, false);
    }

    public boolean canGoBack() {
        return !backStack.isEmpty();
    }

    public void goBack() {
        if (!canGoBack()) {
            return;
        }
        Parent previous = backStack.pop();
        setSceneRoot(previous);
    }

    private void show(Parent root, boolean rememberCurrent) {
        Scene currentScene = stage.getScene();
        if (rememberCurrent && currentScene != null) {
            backStack.push(currentScene.getRoot());
        }
        setSceneRoot(root);
    }

    private void setSceneRoot(Parent root) {
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        String stylesheet = NavigationManager.class.getResource("/css/styles.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }

        FadeTransition transition = new FadeTransition(Duration.millis(180), root);
        transition.setFromValue(0.92);
        transition.setToValue(1.0);
        transition.play();
    }
}
