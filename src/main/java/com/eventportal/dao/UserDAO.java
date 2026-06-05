package com.eventportal.dao;

import com.eventportal.model.Role;
import com.eventportal.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.Optional;

public class UserDAO {
    private final DatabaseConnection databaseConnection;

    public UserDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public User createUser(User user) throws SQLException {
        String sql = """
                INSERT INTO users (username, email, password_hash, role, full_name, department)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindUser(statement, user);
            statement.executeUpdate();
            return readGeneratedUser(statement, id -> findById(connection, id));
        }
    }

    public Optional<User> findById(long id) throws SQLException {
        try (Connection connection = databaseConnection.getConnection()) {
            return findById(connection, id);
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        return findOneBy("username", username);
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        return findOneBy("email", email);
    }

    public User updateUser(User user) throws SQLException {
        String sql = """
                UPDATE users
                SET username = ?, email = ?, password_hash = ?, role = ?, full_name = ?, department = ?
                WHERE id = ?
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindUser(statement, user);
            statement.setLong(7, user.id());
            statement.executeUpdate();
            return findById(connection, user.id()).orElseThrow(() -> new SQLException("User not found after update: " + user.id()));
        }
    }

    private Optional<User> findById(Connection connection, long id) throws SQLException {
        String sql = """
                SELECT id, username, email, password_hash, role, full_name, department, created_at
                FROM users
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapUser(resultSet)) : Optional.empty();
            }
        }
    }

    private Optional<User> findOneBy(String column, String value) throws SQLException {
        String sql = """
                SELECT id, username, email, password_hash, role, full_name, department, created_at
                FROM users
                WHERE %s = ?
                """.formatted(column);

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapUser(resultSet)) : Optional.empty();
            }
        }
    }

    private static void bindUser(PreparedStatement statement, User user) throws SQLException {
        statement.setString(1, user.username());
        statement.setString(2, user.email());
        statement.setString(3, user.passwordHash());
        statement.setString(4, user.role().name());
        statement.setString(5, user.fullName());
        statement.setString(6, user.department());
    }

    private static User readGeneratedUser(PreparedStatement statement, SqlFunction<Long, Optional<User>> finder) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("User insert did not return a generated id");
            }
            long id = keys.getLong(1);
            return finder.apply(id).orElseThrow(() -> new SQLException("User not found after insert: " + id));
        }
    }

    static User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("password_hash"),
                Role.valueOf(resultSet.getString("role")),
                resultSet.getString("full_name"),
                resultSet.getString("department"),
                resultSet.getObject("created_at", OffsetDateTime.class)
        );
    }
}
