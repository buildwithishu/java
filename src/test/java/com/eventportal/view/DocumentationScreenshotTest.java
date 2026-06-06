package com.eventportal.view;

import atlantafx.base.theme.PrimerDark;
import com.eventportal.controller.AuthController;
import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.NavigationManager;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

class DocumentationScreenshotTest extends JavaFxTestSupport {
    @Test
    void generateDocumentationScreenshotsWhenRequested() throws Exception {
        if (!Boolean.getBoolean("generateDocsScreenshots")) {
            return;
        }

        Path outputDirectory = Path.of("screenshots");
        Files.createDirectories(outputDirectory);
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        onFxThread(() -> {
            Stage stage = new Stage();
            NavigationManager navigationManager = new NavigationManager(stage);
            AuthController authController = new AuthController(null, null);
            Parent login = new LoginView(authController, navigationManager, user -> {
            }).build();
            capture(stage, login, outputDirectory.resolve("login.png"));

            Label title = new Label("Browse Events");
            title.getStyleClass().add("screen-title");
            Parent eventList = new EventListView(
                    null,
                    null,
                    navigationManager,
                    student(),
                    events()
            ).build();
            VBox browser = new VBox(18, title, eventList);
            browser.getStyleClass().add("app-root");
            browser.setPadding(new Insets(24));
            capture(stage, browser, outputDirectory.resolve("event-browser.png"));
            stage.close();
            return null;
        });
    }

    private static void capture(Stage stage, Parent root, Path outputPath) throws Exception {
        Scene scene = new Scene(root, 1200, 760);
        String stylesheet = DocumentationScreenshotTest.class.getResource("/css/styles.css").toExternalForm();
        scene.getStylesheets().add(stylesheet);
        stage.setScene(scene);
        stage.show();
        root.applyCss();
        root.layout();

        WritableImage image = scene.snapshot(null);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", outputPath.toFile());
    }

    private static User student() {
        return new User(
                7,
                "student",
                "student@college.edu",
                "hash",
                Role.STUDENT,
                "Student User",
                "Computer Science",
                OffsetDateTime.now()
        );
    }

    private static List<Event> events() {
        OffsetDateTime now = OffsetDateTime.now();
        return List.of(
                event(11, "Code Sprint 2026", EventCategory.TECHNICAL, "Lab Block A", 48, 80, now),
                event(12, "Cultural Night", EventCategory.CULTURAL, "Main Auditorium", 126, 300, now),
                event(13, "AI Systems Seminar", EventCategory.SEMINAR, "Conference Room", 72, 100, now)
        );
    }

    private static Event event(
            long id,
            String title,
            EventCategory category,
            String venue,
            int registeredCount,
            int capacity,
            OffsetDateTime now
    ) {
        return new Event(
                id,
                title,
                "A practical college event open to eligible students.",
                category,
                venue,
                LocalDate.now().plusDays(id),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                capacity,
                registeredCount,
                EventStatus.UPCOMING,
                1,
                now,
                now
        );
    }
}
