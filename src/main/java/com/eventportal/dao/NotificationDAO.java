package com.eventportal.dao;

import com.eventportal.model.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificationDAO {
    private final DatabaseConnection databaseConnection;

    public NotificationDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public Notification create(long userId, String title, String message, Long eventId) throws SQLException {
        String sql = """
                INSERT INTO notifications (user_id, title, message, event_id)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setString(2, title);
            statement.setString(3, message);
            if (eventId == null) {
                statement.setNull(4, java.sql.Types.BIGINT);
            } else {
                statement.setLong(4, eventId);
            }
            statement.executeUpdate();
            return readGeneratedNotification(connection, statement);
        }
    }

    public Optional<Notification> createIfAbsent(long userId, String title, String message, long eventId) throws SQLException {
        String sql = """
                INSERT INTO notifications (user_id, title, message, event_id)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, event_id, title) WHERE title = 'Event Reminder'
                DO NOTHING
                RETURNING id
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, title);
            statement.setString(3, message);
            statement.setLong(4, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return findById(connection, resultSet.getLong("id"));
            }
        }
    }

    public List<Notification> findByUser(long userId) throws SQLException {
        String sql = """
                SELECT id, user_id, title, message, is_read, created_at, event_id
                FROM notifications
                WHERE user_id = ?
                ORDER BY created_at DESC
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            return readNotifications(statement);
        }
    }

    public boolean markAsRead(long notificationId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, notificationId);
            return statement.executeUpdate() > 0;
        }
    }

    public int markAllAsRead(long userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            return statement.executeUpdate();
        }
    }

    public int countUnread(long userId) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS unread_count
                FROM notifications
                WHERE user_id = ? AND is_read = FALSE
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt("unread_count");
            }
        }
    }

    public int deleteOld(int daysOld) throws SQLException {
        String sql = "DELETE FROM notifications WHERE created_at < CURRENT_TIMESTAMP - (? * INTERVAL '1 day')";

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, daysOld);
            return statement.executeUpdate();
        }
    }

    private static Notification readGeneratedNotification(Connection connection, PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("Notification insert did not return a generated id");
            }
            long id = keys.getLong(1);
            String sql = """
                    SELECT id, user_id, title, message, is_read, created_at, event_id
                    FROM notifications
                    WHERE id = ?
                    """;
            try (PreparedStatement findStatement = connection.prepareStatement(sql)) {
                findStatement.setLong(1, id);
                try (ResultSet resultSet = findStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Notification not found after insert: " + id);
                    }
                    return mapNotification(resultSet);
                }
            }
        }
    }

    private static Optional<Notification> findById(Connection connection, long notificationId) throws SQLException {
        String sql = """
                SELECT id, user_id, title, message, is_read, created_at, event_id
                FROM notifications
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, notificationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapNotification(resultSet)) : Optional.empty();
            }
        }
    }

    private static List<Notification> readNotifications(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<Notification> notifications = new ArrayList<>();
            while (resultSet.next()) {
                notifications.add(mapNotification(resultSet));
            }
            return notifications;
        }
    }

    static Notification mapNotification(ResultSet resultSet) throws SQLException {
        long eventId = resultSet.getLong("event_id");
        Long nullableEventId = resultSet.wasNull() ? null : eventId;
        return new Notification(
                resultSet.getLong("id"),
                resultSet.getLong("user_id"),
                resultSet.getString("title"),
                resultSet.getString("message"),
                resultSet.getBoolean("is_read"),
                resultSet.getObject("created_at", OffsetDateTime.class),
                nullableEventId
        );
    }
}
