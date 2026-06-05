package com.eventportal.view;

import com.eventportal.model.User;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ProfileView {
    private final User user;

    public ProfileView(User user) {
        this.user = user;
    }

    public Parent build() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        addRow(grid, 0, "Username", user.username());
        addRow(grid, 1, "Full name", user.fullName());
        addRow(grid, 2, "Email", user.email());
        addRow(grid, 3, "Department", user.department() == null ? "Not set" : user.department());
        addRow(grid, 4, "Role", user.role().name());

        VBox panel = new VBox(14, grid);
        panel.getStyleClass().add("event-card");
        panel.setPadding(new Insets(18));
        return panel;
    }

    private static void addRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("muted-text");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("card-title");
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }
}
