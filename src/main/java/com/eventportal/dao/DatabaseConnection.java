package com.eventportal.dao;

import com.eventportal.util.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public final class DatabaseConnection {
    private static final DatabaseConnection INSTANCE = new DatabaseConnection(AppConfig.load());

    private final AppConfig config;

    DatabaseConnection(AppConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public static DatabaseConnection getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                config.databaseUrl(),
                config.databaseUsername(),
                config.databasePassword()
        );
    }
}
