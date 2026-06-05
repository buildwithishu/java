package com.eventportal.dao;

import com.eventportal.model.Registration;
import com.eventportal.model.RegistrationStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RegistrationDAO {
    private final DatabaseConnection databaseConnection;

    public RegistrationDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public Registration register(long userId, long eventId) throws SQLException {
        String sql = """
                INSERT INTO registrations (user_id, event_id, unique_reg_id, status)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Registration registration = insertRegistration(connection, sql, userId, eventId);
                incrementRegisteredCount(connection, eventId);
                connection.commit();
                return registration;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean cancelRegistration(long registrationId) throws SQLException {
        String lookupSql = """
                SELECT id, user_id, event_id, registration_date, unique_reg_id, status
                FROM registrations
                WHERE id = ?
                """;
        String cancelSql = """
                UPDATE registrations
                SET status = 'CANCELLED'
                WHERE id = ? AND status = 'CONFIRMED'
                """;

        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Optional<Registration> registration = findById(connection, lookupSql, registrationId);
                if (registration.isEmpty() || !registration.get().isActive()) {
                    connection.rollback();
                    return false;
                }

                try (PreparedStatement statement = connection.prepareStatement(cancelSql)) {
                    statement.setLong(1, registrationId);
                    boolean cancelled = statement.executeUpdate() > 0;
                    if (cancelled) {
                        decrementRegisteredCount(connection, registration.get().eventId());
                    }
                    connection.commit();
                    return cancelled;
                }
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<Registration> findByUser(long userId) throws SQLException {
        String sql = """
                SELECT id, user_id, event_id, registration_date, unique_reg_id, status
                FROM registrations
                WHERE user_id = ?
                ORDER BY registration_date DESC
                """;
        return findMany(sql, userId);
    }

    public Optional<Registration> findById(long registrationId) throws SQLException {
        String sql = """
                SELECT id, user_id, event_id, registration_date, unique_reg_id, status
                FROM registrations
                WHERE id = ?
                """;

        try (Connection connection = databaseConnection.getConnection()) {
            return findById(connection, sql, registrationId);
        }
    }

    public List<Registration> findByEvent(long eventId) throws SQLException {
        String sql = """
                SELECT id, user_id, event_id, registration_date, unique_reg_id, status
                FROM registrations
                WHERE event_id = ?
                ORDER BY registration_date DESC
                """;
        return findMany(sql, eventId);
    }

    public boolean isAlreadyRegistered(long userId, long eventId) throws SQLException {
        String sql = """
                SELECT 1
                FROM registrations
                WHERE user_id = ? AND event_id = ? AND status = 'CONFIRMED'
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public int countByEvent(long eventId) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS registration_count
                FROM registrations
                WHERE event_id = ? AND status = 'CONFIRMED'
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt("registration_count");
            }
        }
    }

    public String generateUniqueRegId(long userId, long eventId) {
        return "REG-" + eventId + "-" + userId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Registration insertRegistration(Connection connection, String sql, long userId, long eventId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setLong(2, eventId);
            statement.setString(3, generateUniqueRegId(userId, eventId));
            statement.setString(4, RegistrationStatus.CONFIRMED.name());
            statement.executeUpdate();
            return readGeneratedRegistration(connection, statement);
        }
    }

    private Registration readGeneratedRegistration(Connection connection, PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("Registration insert did not return a generated id");
            }
            long id = keys.getLong(1);
            return findById(connection, """
                    SELECT id, user_id, event_id, registration_date, unique_reg_id, status
                    FROM registrations
                    WHERE id = ?
                    """, id).orElseThrow(() -> new SQLException("Registration not found after insert: " + id));
        }
    }

    private Optional<Registration> findById(Connection connection, String sql, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRegistration(resultSet)) : Optional.empty();
            }
        }
    }

    private List<Registration> findMany(String sql, long id) throws SQLException {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Registration> registrations = new ArrayList<>();
                while (resultSet.next()) {
                    registrations.add(mapRegistration(resultSet));
                }
                return registrations;
            }
        }
    }

    private static void incrementRegisteredCount(Connection connection, long eventId) throws SQLException {
        String sql = """
                UPDATE events
                SET registered_count = registered_count + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND registered_count < capacity AND status = 'UPCOMING'
                """;
        updateEventCount(connection, sql, eventId, "Event is full or unavailable");
    }

    private static void decrementRegisteredCount(Connection connection, long eventId) throws SQLException {
        String sql = """
                UPDATE events
                SET registered_count = registered_count - 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND registered_count > 0
                """;
        updateEventCount(connection, sql, eventId, "Unable to decrement registration count");
    }

    private static void updateEventCount(Connection connection, String sql, long eventId, String failureMessage) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException(failureMessage + ": " + eventId);
            }
        }
    }

    static Registration mapRegistration(ResultSet resultSet) throws SQLException {
        return new Registration(
                resultSet.getLong("id"),
                resultSet.getLong("user_id"),
                resultSet.getLong("event_id"),
                resultSet.getObject("registration_date", OffsetDateTime.class),
                resultSet.getString("unique_reg_id"),
                RegistrationStatus.valueOf(resultSet.getString("status"))
        );
    }
}
