package com.eventportal.view;

import com.eventportal.controller.AuthController;
import com.eventportal.controller.CertificateController;
import com.eventportal.controller.DashboardController;
import com.eventportal.controller.EventController;
import com.eventportal.controller.NotificationController;
import com.eventportal.controller.RegistrationController;
import com.eventportal.model.DashboardSummary;
import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.NavigationManager;
import com.eventportal.util.SessionManager;
import javafx.scene.Parent;
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
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewBuildTest extends JavaFxTestSupport {
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
    @Mock
    private DashboardController dashboardController;

    private NavigationManager navigationManager;
    private Parent loginRoot;

    @BeforeEach
    void setUp() throws Exception {
        navigationManager = onFxThread(() -> new NavigationManager(new Stage()));
        loginRoot = onFxThread(StackPane::new);
    }

    @Test
    void authenticationViews_shouldBuild() throws Exception {
        assertBuilds(() -> new LoginView(authController, navigationManager, user -> {
        }).build());
        assertBuilds(() -> new RegisterView(authController, navigationManager).build());
    }

    @Test
    void studentViews_shouldBuild() throws Exception {
        User student = user(7, Role.STUDENT);
        Event upcoming = event(EventStatus.UPCOMING);
        when(eventController.findUpcomingEvents()).thenReturn(List.of(upcoming));
        when(eventController.findOrganizerForEvent(upcoming)).thenReturn(user(1, Role.ADMIN));
        when(registrationController.findMyRegistrationDetails()).thenReturn(List.of());
        when(certificateController.findMyCertificates()).thenReturn(List.of());

        assertBuilds(() -> new StudentDashboardView(
                student,
                eventController,
                registrationController,
                certificateController,
                notificationController,
                navigationManager,
                SessionManager.getInstance(),
                loginRoot
        ).build());
        assertBuilds(() -> new EventListView(
                registrationController,
                eventController,
                navigationManager,
                student,
                List.of(upcoming)
        ).build());
        assertBuilds(() -> new EventDetailView(
                upcoming,
                eventController,
                student,
                registrationController,
                navigationManager
        ).build());
        assertBuilds(() -> new MyRegistrationsView(registrationController, certificateController).build());
        assertBuilds(() -> new CertificateView(certificateController).build());
        assertBuilds(() -> new NotificationView(notificationController, student).build());
        assertBuilds(() -> new ProfileView(student).build());
    }

    @Test
    void adminViews_shouldBuild() throws Exception {
        User admin = user(1, Role.ADMIN);
        Event upcoming = event(EventStatus.UPCOMING);
        DashboardSummary summary = new DashboardSummary(0, 0, 0, List.of(), List.of(), List.of());
        when(dashboardController.loadAdminSummary()).thenReturn(summary);
        when(eventController.findAllEvents()).thenReturn(List.of(upcoming));
        when(eventController.findOrganizerForEvent(upcoming)).thenReturn(admin);

        assertBuilds(() -> new AdminDashboardView(
                admin,
                dashboardController,
                eventController,
                registrationController,
                navigationManager,
                SessionManager.getInstance(),
                loginRoot
        ).build());
        assertBuilds(() -> new AdminEventManagementView(
                eventController,
                registrationController,
                admin,
                navigationManager
        ).build());
        assertBuilds(() -> new AdminReportView(dashboardController, navigationManager).build());
        assertBuilds(() -> new EventFormView(eventController, () -> {
        }).build());
        assertBuilds(() -> new EventFormView(eventController, () -> {
        }, upcoming).build());
        assertBuilds(() -> new EventDetailView(
                upcoming,
                eventController,
                admin,
                registrationController,
                navigationManager
        ).build());
    }

    private static void assertBuilds(Callable<Parent> builder) throws Exception {
        onFxThread(() -> {
            Parent root = builder.call();
            assertThat(root).isNotNull();
            return null;
        });
    }

    private static User user(long id, Role role) {
        return new User(
                id,
                role == Role.ADMIN ? "admin" : "student",
                role == Role.ADMIN ? "admin@college.edu" : "student@college.edu",
                "hash",
                role,
                role == Role.ADMIN ? "Portal Administrator" : "Student User",
                role == Role.ADMIN ? "Administration" : "Computer Science",
                OffsetDateTime.now()
        );
    }

    private static Event event(EventStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Event(
                11,
                "Architecture Workshop",
                "A practical system design workshop.",
                EventCategory.WORKSHOP,
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
}
