package com.eventportal.dao;

import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventDAO {
    private final DatabaseConnection databaseConnection;

    public EventDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public Event createEvent(Event event) throws SQLException {
        String sql = """
                INSERT INTO events (title, description, category, venue, event_date, start_time, end_time, capacity, registered_count, status, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEvent(statement, event);
            statement.executeUpdate();
            return readGeneratedEvent(statement, id -> findById(connection, id));
        }
    }

    public Event updateEvent(Event event) throws SQLException {
        String sql = """
                UPDATE events
                SET title = ?, description = ?, category = ?, venue = ?, event_date = ?, start_time = ?,
                    end_time = ?, capacity = ?, registered_count = ?, status = ?, created_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEvent(statement, event);
            statement.setLong(12, event.id());
            statement.executeUpdate();
            return findById(connection, event.id()).orElseThrow(() -> new SQLException("Event not found after update: " + event.id()));
        }
    }

    public boolean deleteEvent(long id) throws SQLException {
        String sql = """
                DELETE FROM events event
                WHERE event.id = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM registrations registration
                      WHERE registration.event_id = event.id
                  )
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<Event> findById(long id) throws SQLException {
        try (Connection connection = databaseConnection.getConnection()) {
            return findById(connection, id);
        }
    }

    public List<Event> findAll() throws SQLException {
        return findByQuery("""
                SELECT id, title, description, category, venue, event_date, start_time, end_time,
                       capacity, registered_count, status, created_by, created_at, updated_at
                FROM events
                ORDER BY event_date, start_time
                """);
    }

    public List<Event> findByCategory(EventCategory category) throws SQLException {
        return findByFilter("category", category.name());
    }

    public List<Event> findByStatus(EventStatus status) throws SQLException {
        return findByFilter("status", status.name());
    }

    public List<Event> findUpcoming() throws SQLException {
        return findByStatus(EventStatus.UPCOMING);
    }

    public List<Event> searchByKeyword(String keyword) throws SQLException {
        String sql = """
                SELECT id, title, description, category, venue, event_date, start_time, end_time,
                       capacity, registered_count, status, created_by, created_at, updated_at
                FROM events
                WHERE LOWER(title) LIKE LOWER(?) OR LOWER(description) LIKE LOWER(?) OR LOWER(venue) LIKE LOWER(?)
                ORDER BY event_date, start_time
                """;
        String pattern = "%" + keyword + "%";

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            return readEvents(statement);
        }
    }

    public boolean incrementRegisteredCount(long eventId) throws SQLException {
        String sql = """
                UPDATE events
                SET registered_count = registered_count + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND registered_count < capacity
                """;
        return updateCount(eventId, sql);
    }

    public boolean decrementRegisteredCount(long eventId) throws SQLException {
        String sql = """
                UPDATE events
                SET registered_count = registered_count - 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND registered_count > 0
                """;
        return updateCount(eventId, sql);
    }

    private Optional<Event> findById(Connection connection, long id) throws SQLException {
        String sql = """
                SELECT id, title, description, category, venue, event_date, start_time, end_time,
                       capacity, registered_count, status, created_by, created_at, updated_at
                FROM events
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapEvent(resultSet)) : Optional.empty();
            }
        }
    }

    private List<Event> findByFilter(String column, String value) throws SQLException {
        String sql = """
                SELECT id, title, description, category, venue, event_date, start_time, end_time,
                       capacity, registered_count, status, created_by, created_at, updated_at
                FROM events
                WHERE %s = ?
                ORDER BY event_date, start_time
                """.formatted(column);

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            return readEvents(statement);
        }
    }

    private List<Event> findByQuery(String sql) throws SQLException {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return readEvents(statement);
        }
    }

    private boolean updateCount(long eventId, String sql) throws SQLException {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            return statement.executeUpdate() > 0;
        }
    }

    private static void bindEvent(PreparedStatement statement, Event event) throws SQLException {
        statement.setString(1, event.title());
        statement.setString(2, event.description());
        statement.setString(3, event.category().name());
        statement.setString(4, event.venue());
        statement.setDate(5, Date.valueOf(event.eventDate()));
        statement.setTime(6, Time.valueOf(event.startTime()));
        statement.setTime(7, Time.valueOf(event.endTime()));
        statement.setInt(8, event.capacity());
        statement.setInt(9, event.registeredCount());
        statement.setString(10, event.status().name());
        statement.setLong(11, event.createdBy());
    }

    private static Event readGeneratedEvent(PreparedStatement statement, SqlFunction<Long, Optional<Event>> finder) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("Event insert did not return a generated id");
            }
            long id = keys.getLong(1);
            return finder.apply(id).orElseThrow(() -> new SQLException("Event not found after insert: " + id));
        }
    }

    private static List<Event> readEvents(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<Event> events = new ArrayList<>();
            while (resultSet.next()) {
                events.add(mapEvent(resultSet));
            }
            return events;
        }
    }

    static Event mapEvent(ResultSet resultSet) throws SQLException {
        return new Event(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                EventCategory.valueOf(resultSet.getString("category")),
                resultSet.getString("venue"),
                resultSet.getDate("event_date").toLocalDate(),
                resultSet.getTime("start_time").toLocalTime(),
                resultSet.getTime("end_time").toLocalTime(),
                resultSet.getInt("capacity"),
                resultSet.getInt("registered_count"),
                EventStatus.valueOf(resultSet.getString("status")),
                resultSet.getLong("created_by"),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class)
        );
    }
}
