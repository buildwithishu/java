package com.eventportal.dao;

import com.eventportal.model.Certificate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CertificateDAO {
    private final DatabaseConnection databaseConnection;

    public CertificateDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public Certificate saveCertificate(long registrationId, String certificatePath) throws SQLException {
        String sql = """
                INSERT INTO certificates (registration_id, certificate_path)
                VALUES (?, ?)
                ON CONFLICT (registration_id)
                DO UPDATE SET certificate_path = EXCLUDED.certificate_path, generated_at = CURRENT_TIMESTAMP
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, registrationId);
            statement.setString(2, certificatePath);
            statement.executeUpdate();
            return findByRegistration(connection, registrationId)
                    .orElseThrow(() -> new SQLException("Certificate not found after save for registration: " + registrationId));
        }
    }

    public Optional<Certificate> findByRegistration(long registrationId) throws SQLException {
        try (Connection connection = databaseConnection.getConnection()) {
            return findByRegistration(connection, registrationId);
        }
    }

    public List<Certificate> findByUser(long userId) throws SQLException {
        String sql = """
                SELECT c.id, c.registration_id, c.certificate_path, c.generated_at
                FROM certificates c
                JOIN registrations r ON r.id = c.registration_id
                WHERE r.user_id = ?
                ORDER BY c.generated_at DESC
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Certificate> certificates = new ArrayList<>();
                while (resultSet.next()) {
                    certificates.add(mapCertificate(resultSet));
                }
                return certificates;
            }
        }
    }

    private Optional<Certificate> findByRegistration(Connection connection, long registrationId) throws SQLException {
        String sql = """
                SELECT id, registration_id, certificate_path, generated_at
                FROM certificates
                WHERE registration_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, registrationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapCertificate(resultSet)) : Optional.empty();
            }
        }
    }

    static Certificate mapCertificate(ResultSet resultSet) throws SQLException {
        return new Certificate(
                resultSet.getLong("id"),
                resultSet.getLong("registration_id"),
                resultSet.getString("certificate_path"),
                resultSet.getObject("generated_at", OffsetDateTime.class)
        );
    }
}
