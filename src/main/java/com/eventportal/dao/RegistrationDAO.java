package com.eventportal.dao;

import com.eventportal.model.Registration;
import com.eventportal.model.RegistrationDetail;
import com.eventportal.model.RegistrationStatus;
import com.eventportal.model.MonthlyRegistrationStat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.YearMonth;
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

    public List<RegistrationDetail> findDetailsByUser(long userId) throws SQLException {
        String sql = """
                SELECT r.id,
                       r.event_id,
                       r.registration_date,
                       r.unique_reg_id,
                       r.status,
                       e.title AS event_title,
                       e.event_date,
                       e.venue
                FROM registrations r
                JOIN events e ON e.id = r.event_id
                WHERE r.user_id = ?
                ORDER BY r.registration_date DESC
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RegistrationDetail> registrations = new ArrayList<>();
                while (resultSet.next()) {
                    registrations.add(mapRegistrationDetail(resultSet));
                }
                return registrations;
            }
        }
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

    public int countConfirmed() throws SQLException {
        String sql = """
                SELECT COUNT(*) AS registration_count
                FROM registrations
                WHERE status = 'CONFIRMED'
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt("registration_count");
        }
    }

    public List<MonthlyRegistrationStat> countConfirmedByMonth() throws SQLException {
        String sql = """
                SELECT EXTRACT(YEAR FROM registration_date)::INT AS registration_year,
                       EXTRACT(MONTH FROM registration_date)::INT AS registration_month,
                       COUNT(*) AS registration_count
                FROM registrations
                WHERE status = 'CONFIRMED'
                GROUP BY registration_year, registration_month
                ORDER BY registration_year, registration_month
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<MonthlyRegistrationStat> stats = new ArrayList<>();
            while (resultSet.next()) {
                stats.add(new MonthlyRegistrationStat(
                        YearMonth.of(resultSet.getInt("registration_year"), resultSet.getInt("registration_month")),
                        resultSet.getLong("registration_count")
                ));
            }
            return stats;
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

    static RegistrationDetail mapRegistrationDetail(ResultSet resultSet) throws SQLException {
        return new RegistrationDetail(
                resultSet.getLong("id"),
                resultSet.getLong("event_id"),
                resultSet.getString("unique_reg_id"),
                RegistrationStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("registration_date", OffsetDateTime.class),
                resultSet.getString("event_title"),
                resultSet.getDate("event_date").toLocalDate(),
                resultSet.getString("venue")
        );
    }
}
