package com.eventportal.dao;

import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RegistrationDaoIntegrationTest {
    private DatabaseConnection databaseConnection;
    private UserDAO userDAO;
    private EventDAO eventDAO;
    private RegistrationDAO registrationDAO;

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
        OffsetDateTime now = OffsetDateTime.now();
        return new Event(
                0,
                "Integration Test Event",
                "Verifies registration transaction behavior.",
                EventCategory.TECHNICAL,
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
