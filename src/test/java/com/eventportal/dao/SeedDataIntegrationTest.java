package com.eventportal.dao;

import com.eventportal.util.PasswordHasher;
import com.eventportal.util.TestAppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SeedDataIntegrationTest {
    private DatabaseConnection databaseConnection;

    @BeforeEach
    void setUp() throws SQLException {
        String databaseUrl = System.getenv("APP_DB_URL");
        assumeTrue(databaseUrl != null && !databaseUrl.isBlank(), "APP_DB_URL is required for seed integration tests");
        databaseConnection = new DatabaseConnection(TestAppConfig.withDatabase(
                databaseUrl,
                System.getenv().getOrDefault("APP_DB_USER", "postgres"),
                System.getenv().getOrDefault("APP_DB_PASSWORD", "postgres123")
        ));
        resetDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (databaseConnection != null) {
            resetDatabase();
            databaseConnection.close();
        }
    }

    @Test
    void seedScript_shouldCreateCompleteIdempotentSampleDataset() throws SQLException, IOException {
        executeScript(Path.of("sql/schema.sql"));
        executeScript(Path.of("sql/seed.sql"));
        executeScript(Path.of("sql/seed.sql"));

        assertThat(queryCount("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'")).isEqualTo(1);
        assertThat(queryCount("SELECT COUNT(*) FROM users WHERE role = 'STUDENT'")).isEqualTo(2);
        assertThat(queryCount("SELECT COUNT(*) FROM events")).isEqualTo(5);
        assertThat(queryCount("SELECT COUNT(*) FROM registrations WHERE status = 'CONFIRMED'")).isEqualTo(2);
        assertThat(queryCount("SELECT COUNT(*) FROM notifications")).isEqualTo(4);
        assertThat(queryCount("""
                SELECT COUNT(*)
                FROM events e
                WHERE e.registered_count = (
                    SELECT COUNT(*)
                    FROM registrations r
                    WHERE r.event_id = e.id AND r.status = 'CONFIRMED'
                )
                """)).isEqualTo(5);

        UserDAO userDAO = new UserDAO(databaseConnection);
        assertThat(userDAO.findByUsername("admin"))
                .get()
                .matches(user -> PasswordHasher.verify("admin123", user.passwordHash()));
        assertThat(userDAO.findByUsername("student1"))
                .get()
                .matches(user -> PasswordHasher.verify("admin123", user.passwordHash()));
        assertThat(userDAO.findByUsername("student2"))
                .get()
                .matches(user -> PasswordHasher.verify("admin123", user.passwordHash()));
    }

    private void executeScript(Path path) throws IOException, SQLException {
        try (Connection connection = databaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(Files.readString(path));
        }
    }

    private int queryCount(String sql) throws SQLException {
        try (Connection connection = databaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
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
}
