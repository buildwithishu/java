package com.eventportal.dao;

import com.eventportal.model.Certificate;
import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
import com.eventportal.model.Notification;
import com.eventportal.model.Registration;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.PasswordHasher;
import com.eventportal.util.TestAppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CoreDaoIntegrationTest {
    private DatabaseConnection databaseConnection;
    private UserDAO userDAO;
    private EventDAO eventDAO;
    private RegistrationDAO registrationDAO;
    private CertificateDAO certificateDAO;
    private NotificationDAO notificationDAO;

    @BeforeEach
    void setUp() throws SQLException {
        String databaseUrl = System.getenv("APP_DB_URL");
        assumeTrue(databaseUrl != null && !databaseUrl.isBlank(), "APP_DB_URL is required for DAO integration tests");

        databaseConnection = new DatabaseConnection(TestAppConfig.withDatabase(
                databaseUrl,
                System.getenv().getOrDefault("APP_DB_USER", "postgres"),
                System.getenv().getOrDefault("APP_DB_PASSWORD", "postgres123")
        ));
        resetDatabase();
        userDAO = new UserDAO(databaseConnection);
        eventDAO = new EventDAO(databaseConnection);
        registrationDAO = new RegistrationDAO(databaseConnection);
        certificateDAO = new CertificateDAO(databaseConnection);
        notificationDAO = new NotificationDAO(databaseConnection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (databaseConnection != null) {
            resetDatabase();
        }
    }

    @Test
    void registerAndCancel_shouldKeepRegistrationAndEventCountConsistent() throws SQLException {
        User admin = userDAO.createUser(user("admin", "admin@college.edu", Role.ADMIN));
        User student = userDAO.createUser(user("student", "student@college.edu", Role.STUDENT));
        Event event = eventDAO.createEvent(event(admin.id()));

        Registration registration = registrationDAO.register(student.id(), event.id());

        assertThat(registrationDAO.isAlreadyRegistered(student.id(), event.id())).isTrue();
        assertThat(registrationDAO.countByEvent(event.id())).isEqualTo(1);
        assertThat(eventDAO.findById(event.id())).get()
                .extracting(Event::registeredCount)
                .isEqualTo(1);

        assertThatThrownBy(() -> registrationDAO.register(student.id(), event.id()))
                .isInstanceOf(SQLException.class);
        assertThat(registrationDAO.countByEvent(event.id())).isEqualTo(1);
        assertThat(eventDAO.findById(event.id())).get()
                .extracting(Event::registeredCount)
                .isEqualTo(1);

        assertThat(registrationDAO.cancelRegistration(registration.id())).isTrue();
        assertThat(registrationDAO.isAlreadyRegistered(student.id(), event.id())).isFalse();
        assertThat(registrationDAO.countByEvent(event.id())).isZero();
        assertThat(eventDAO.findById(event.id())).get()
                .extracting(Event::registeredCount)
                .isEqualTo(0);
    }

    @Test
    void usersAndEvents_shouldSupportCrudSearchAndFilters() throws SQLException {
        User admin = userDAO.createUser(user("admin", "admin@college.edu", Role.ADMIN));
        User student = userDAO.createUser(user("student", "student@college.edu", Role.STUDENT));

        assertThat(userDAO.findByUsername("student")).contains(student);
        assertThat(userDAO.findByEmail("student@college.edu")).contains(student);
        assertThat(userDAO.countByRole(Role.STUDENT)).isEqualTo(1);

        User updatedStudent = new User(
                student.id(),
                student.username(),
                student.email(),
                student.passwordHash(),
                student.role(),
                "Updated Student",
                "Electronics",
                student.createdAt()
        );
        assertThat(userDAO.updateUser(updatedStudent))
                .extracting(User::fullName, User::department)
                .containsExactly("Updated Student", "Electronics");

        Event technical = eventDAO.createEvent(event(
                admin.id(),
                "Integration Code Sprint",
                EventCategory.TECHNICAL
        ));
        Event cultural = eventDAO.createEvent(event(
                admin.id(),
                "Campus Cultural Night",
                EventCategory.CULTURAL
        ));

        assertThat(eventDAO.findAll()).hasSize(2);
        assertThat(eventDAO.searchByKeyword("code")).extracting(Event::id).containsExactly(technical.id());
        assertThat(eventDAO.searchByKeyword("campus")).extracting(Event::id).containsExactly(cultural.id());
        assertThat(eventDAO.findByCategory(EventCategory.TECHNICAL))
                .extracting(Event::id)
                .containsExactly(technical.id());
        assertThat(eventDAO.findUpcoming()).hasSize(2);

        Event ongoing = new Event(
                technical.id(),
                "Updated Integration Sprint",
                technical.description(),
                technical.category(),
                technical.venue(),
                technical.eventDate(),
                technical.startTime(),
                technical.endTime(),
                technical.capacity(),
                technical.registeredCount(),
                EventStatus.ONGOING,
                technical.createdBy(),
                technical.createdAt(),
                technical.updatedAt()
        );
        assertThat(eventDAO.updateEvent(ongoing))
                .extracting(Event::title, Event::status)
                .containsExactly("Updated Integration Sprint", EventStatus.ONGOING);
        assertThat(eventDAO.findByStatus(EventStatus.ONGOING))
                .extracting(Event::id)
                .containsExactly(technical.id());

        assertThat(eventDAO.deleteEvent(cultural.id())).isTrue();
        assertThat(eventDAO.findById(cultural.id())).isEmpty();
    }

    @Test
    void certificatesNotificationsAndAggregates_shouldRoundTrip() throws SQLException {
        User admin = userDAO.createUser(user("admin", "admin@college.edu", Role.ADMIN));
        User student = userDAO.createUser(user("student", "student@college.edu", Role.STUDENT));
        Event event = eventDAO.createEvent(event(admin.id()));
        Registration registration = registrationDAO.register(student.id(), event.id());

        assertThat(registrationDAO.findDetailsByUser(student.id())).singleElement()
                .satisfies(detail -> {
                    assertThat(detail.eventTitle()).isEqualTo(event.title());
                    assertThat(detail.venue()).isEqualTo(event.venue());
                    assertThat(detail.isActive()).isTrue();
                });
        assertThat(registrationDAO.countConfirmed()).isEqualTo(1);
        assertThat(registrationDAO.countConfirmedByMonth()).singleElement()
                .satisfies(stat -> {
                    assertThat(stat.month()).isEqualTo(YearMonth.now());
                    assertThat(stat.registrationCount()).isEqualTo(1);
                });
        assertThat(userDAO.findRegisteredUsersByEvent(event.id()))
                .extracting(User::id)
                .containsExactly(student.id());

        Notification welcome = notificationDAO.create(
                student.id(),
                "Welcome",
                "Your registration is active.",
                null
        );
        assertThat(welcome.eventId()).isNull();
        assertThat(notificationDAO.createIfAbsent(
                student.id(),
                "Event Reminder",
                "Starts tomorrow.",
                event.id()
        )).isPresent();
        assertThat(notificationDAO.createIfAbsent(
                student.id(),
                "Event Reminder",
                "Starts tomorrow.",
                event.id()
        )).isEmpty();
        assertThat(notificationDAO.countUnread(student.id())).isEqualTo(2);
        assertThat(notificationDAO.markAsRead(welcome.id())).isTrue();
        assertThat(notificationDAO.countUnread(student.id())).isEqualTo(1);
        assertThat(notificationDAO.markAllAsRead(student.id())).isEqualTo(1);
        assertThat(notificationDAO.findByUser(student.id()))
                .allMatch(Notification::read);

        Certificate created = certificateDAO.saveCertificate(registration.id(), "/tmp/first.pdf");
        Certificate updated = certificateDAO.saveCertificate(registration.id(), "/tmp/updated.pdf");
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(certificateDAO.findByRegistration(registration.id()))
                .get()
                .extracting(Certificate::certificatePath)
                .isEqualTo("/tmp/updated.pdf");
        assertThat(certificateDAO.findByUser(student.id()))
                .extracting(Certificate::registrationId)
                .containsExactly(registration.id());
    }

    private void resetDatabase() throws SQLException {
        try (Connection connection = databaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    TRUNCATE TABLE notifications, certificates, registrations, events, users
                    RESTART IDENTITY CASCADE
                    """);
        }
    }

    private static User user(String username, String email, Role role) {
        return new User(
                0,
                username,
                email,
                PasswordHasher.hash("secure123"),
                role,
                role == Role.ADMIN ? "Portal Administrator" : "Student User",
                role == Role.ADMIN ? "Administration" : "Computer Science",
                OffsetDateTime.now()
        );
    }

    private static Event event(long adminId) {
        return event(adminId, "Integration Test Event", EventCategory.TECHNICAL);
    }

    private static Event event(long adminId, String title, EventCategory category) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Event(
                0,
                title,
                "Verifies registration transaction behavior.",
                category,
                "Test Lab",
                LocalDate.now().plusDays(7),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                2,
                0,
                EventStatus.UPCOMING,
                adminId,
                now,
                now
        );
    }
}
