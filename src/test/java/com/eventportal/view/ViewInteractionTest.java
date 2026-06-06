package com.eventportal.view;

import com.eventportal.App;
import com.eventportal.controller.AuthController;
import com.eventportal.controller.CertificateController;
import com.eventportal.controller.EventController;
import com.eventportal.controller.NotificationController;
import com.eventportal.controller.RegistrationController;
import com.eventportal.model.Certificate;
import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
import com.eventportal.model.Notification;
import com.eventportal.model.RegistrationDetail;
import com.eventportal.model.RegistrationStatus;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.NavigationManager;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ViewInteractionTest extends JavaFxTestSupport {
    @Mock
    private AuthController authController;
    @Mock
    private EventController eventController;
    @Mock
    private RegistrationController registrationController;
    @Mock
    private CertificateController certificateController;
    @Mock
    private NotificationController notificationController;

    private NavigationManager navigationManager;

    @BeforeEach
    void setUp() throws Exception {
        navigationManager = onFxThread(() -> new NavigationManager(new Stage()));
    }

    @Test
    void loginAndRegistration_shouldCompleteThroughAsyncHandlers() throws Exception {
        User student = user();
        AtomicReference<User> loggedIn = new AtomicReference<>();
        when(authController.login("student", "secure123")).thenReturn(student);
        when(authController.registerStudent(
                "newstudent",
                "new@college.edu",
                "secure123",
                "New Student",
                "Computer Science"
        )).thenReturn(student);

        Parent login = onFxThread(() -> new LoginView(
                authController,
                navigationManager,
                loggedIn::set
        ).build());
        onFxThread(() -> {
            textField(login, "Username").setText("student");
            passwordField(login).setText("secure123");
            button(login, "Login").fire();
            return null;
        });
        awaitFx(() -> loggedIn.get() != null);
        assertThat(loggedIn.get()).isEqualTo(student);

        Parent register = onFxThread(() -> new RegisterView(authController, navigationManager).build());
        onFxThread(() -> {
            textField(register, "Username").setText("newstudent");
            textField(register, "Email").setText("new@college.edu");
            passwordField(register).setText("secure123");
            textField(register, "Full name").setText("New Student");
            comboBox(register).setValue("Computer Science");
            button(register, "Register").fire();
            return null;
        });
        awaitFx(() -> hasLabel(register, "Account created. You can now log in."));
        verify(authController).registerStudent(
                "newstudent",
                "new@college.edu",
                "secure123",
                "New Student",
                "Computer Science"
        );
    }

    @Test
    void eventListAndForm_shouldReactToUserInput() throws Exception {
        Event workshop = event("Architecture Workshop", EventCategory.WORKSHOP, EventStatus.UPCOMING);
        Event cultural = event("Cultural Night", EventCategory.CULTURAL, EventStatus.UPCOMING);
        Parent list = onFxThread(() -> new EventListView(
                registrationController,
                eventController,
                navigationManager,
                user(),
                List.of(workshop, cultural)
        ).build());

        onFxThread(() -> {
            textField(list, "Search events").setText("cultural");
            return null;
        });
        assertThat(onFxThread(() -> labels(list))).contains("Cultural Night").doesNotContain("Architecture Workshop");

        Parent form = onFxThread(() -> new EventFormView(eventController, () -> {
        }).build());
        onFxThread(() -> {
            textField(form, "Title").setText("New Event");
            textArea(form).setText("Description");
            eventCategoryBox(form).setValue(EventCategory.TECHNICAL);
            textField(form, "Venue").setText("Lab");
            datePicker(form).setValue(LocalDate.now().plusDays(2));
            textField(form, "Start HH:mm").setText("invalid");
            textField(form, "End HH:mm").setText("12:00");
            spinner(form).getValueFactory().setValue(30);
            button(form, "Create").fire();
            return null;
        });
        assertThat(onFxThread(() -> labels(form)))
                .anyMatch(text -> text.contains("could not be parsed"));
    }

    @Test
    void registrationAndCertificateViews_shouldRenderDataAndRunActions() throws Exception {
        RegistrationDetail detail = new RegistrationDetail(
                21,
                11,
                "REG-21",
                RegistrationStatus.CONFIRMED,
                OffsetDateTime.now(),
                "Architecture Workshop",
                EventStatus.COMPLETED,
                LocalDate.now().minusDays(1),
                "Seminar Hall"
        );
        Certificate certificate = new Certificate(5, 21, "/missing/certificate.pdf", OffsetDateTime.now());
        when(registrationController.findMyRegistrationDetails()).thenReturn(List.of(detail));
        when(certificateController.generateForRegistration(21)).thenReturn(certificate);
        when(certificateController.findMyCertificates()).thenReturn(List.of(certificate));
        when(certificateController.resolveCertificatePath(certificate))
                .thenThrow(new java.io.IOException("Certificate file does not exist"));

        Parent registrations = onFxThread(() ->
                new MyRegistrationsView(registrationController, certificateController).build());
        awaitFx(() -> hasButton(registrations, "Generate Certificate"));
        onFxThread(() -> {
            button(registrations, "Generate Certificate").fire();
            return null;
        });
        awaitFx(() -> hasLabel(registrations, "Certificate generated."));
        verify(certificateController).generateForRegistration(21);

        Parent certificates = onFxThread(() -> new CertificateView(certificateController).build());
        awaitFx(() -> hasButton(certificates, "Open PDF"));
        onFxThread(() -> {
            button(certificates, "Open PDF").fire();
            return null;
        });
        assertThat(onFxThread(() -> labels(certificates)))
                .anyMatch(text -> text.contains("does not exist"));
    }

    @Test
    void notificationView_shouldMarkIndividualAndAllNotificationsRead() throws Exception {
        User student = user();
        Notification unread = notification(false);
        Notification read = notification(true);
        when(notificationController.findByUser(student.id()))
                .thenReturn(List.of(unread), List.of(read), List.of(read));
        when(notificationController.markAsRead(unread.id())).thenReturn(true);
        when(notificationController.markAllAsRead(student.id())).thenReturn(1);

        Parent root = onFxThread(() -> new NotificationView(notificationController, student).build());
        awaitFx(() -> hasLabel(root, "Unread notifications: 1"));
        onFxThread(() -> {
            button(root, "Mark read").fire();
            return null;
        });
        awaitFx(() -> hasLabel(root, "Unread notifications: 0"));

        onFxThread(() -> {
            button(root, "Mark all as read").fire();
            return null;
        });
        awaitFx(() -> !button(root, "Mark all as read").isDisabled());
        verify(notificationController).markAsRead(unread.id());
        verify(notificationController).markAllAsRead(student.id());
    }

    @Test
    void navigationManager_shouldRememberAndRestoreRoots() throws Exception {
        onFxThread(() -> {
            Stage stage = new Stage();
            NavigationManager manager = new NavigationManager(stage);
            Parent first = new StackPane(new Label("First"));
            Parent second = new StackPane(new Label("Second"));

            manager.replace(first);
            manager.show(second);
            assertThat(stage.getScene().getRoot()).isSameAs(second);
            assertThat(manager.canGoBack()).isTrue();

            manager.goBack();
            assertThat(stage.getScene().getRoot()).isSameAs(first);
            assertThat(manager.canGoBack()).isFalse();
            manager.goBack();
            return null;
        });
    }

    @Test
    void application_shouldStartWithConfiguredLoginStage() throws Exception {
        onFxThread(() -> {
            Stage stage = new Stage();
            new App().start(stage);

            assertThat(stage.isShowing()).isTrue();
            assertThat(stage.getTitle()).isEqualTo(App.APP_NAME);
            assertThat(stage.getMinWidth()).isEqualTo(900);
            assertThat(stage.getMinHeight()).isEqualTo(600);
            assertThat(stage.getScene()).isNotNull();
            assertThat(labels(stage.getScene().getRoot()))
                    .contains("College Event Management Portal");
            stage.close();
            return null;
        });
    }

    private static Button button(Parent root, String text) {
        return nodes(root).stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(candidate -> text.equals(candidate.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Button not found: " + text));
    }

    private static boolean hasButton(Parent root, String text) {
        return nodes(root).stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .anyMatch(candidate -> text.equals(candidate.getText()));
    }

    private static TextField textField(Parent root, String prompt) {
        return nodes(root).stream()
                .filter(TextField.class::isInstance)
                .map(TextField.class::cast)
                .filter(field -> prompt.equals(field.getPromptText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Text field not found: " + prompt));
    }

    private static PasswordField passwordField(Parent root) {
        return nodes(root).stream()
                .filter(PasswordField.class::isInstance)
                .map(PasswordField.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static ComboBox<String> comboBox(Parent root) {
        return nodes(root).stream()
                .filter(ComboBox.class::isInstance)
                .map(node -> (ComboBox<String>) node)
                .findFirst()
                .orElseThrow();
    }

    private static ComboBox<EventCategory> eventCategoryBox(Parent root) {
        return nodes(root).stream()
                .filter(ComboBox.class::isInstance)
                .map(node -> (ComboBox<EventCategory>) node)
                .findFirst()
                .orElseThrow();
    }

    private static TextArea textArea(Parent root) {
        return nodes(root).stream().filter(TextArea.class::isInstance).map(TextArea.class::cast).findFirst().orElseThrow();
    }

    private static DatePicker datePicker(Parent root) {
        return nodes(root).stream().filter(DatePicker.class::isInstance).map(DatePicker.class::cast).findFirst().orElseThrow();
    }

    private static Spinner<Integer> spinner(Parent root) {
        return nodes(root).stream()
                .filter(Spinner.class::isInstance)
                .map(node -> (Spinner<Integer>) node)
                .findFirst()
                .orElseThrow();
    }

    private static boolean hasLabel(Parent root, String text) {
        return labels(root).contains(text);
    }

    private static List<String> labels(Parent root) {
        return nodes(root).stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .map(Label::getText)
                .toList();
    }

    private static List<Node> nodes(Parent root) {
        List<Node> result = new ArrayList<>();
        collect(root, result);
        return result;
    }

    private static void collect(Node node, List<Node> result) {
        result.add(node);
        if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            collect(scrollPane.getContent(), result);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collect(child, result);
            }
        }
    }

    private static User user() {
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

    private static Event event(String title, EventCategory category, EventStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Event(
                11,
                title,
                "Event description.",
                category,
                "Seminar Hall",
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                50,
                4,
                status,
                1,
                now,
                now
        );
    }

    private static Notification notification(boolean read) {
        return new Notification(
                31,
                7,
                "Event reminder",
                "The event starts tomorrow.",
                read,
                OffsetDateTime.now(),
                11L
        );
    }
}
