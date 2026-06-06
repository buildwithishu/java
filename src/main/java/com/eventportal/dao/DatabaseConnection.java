package com.eventportal.dao;

import com.eventportal.util.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public final class DatabaseConnection implements AutoCloseable {
    private static final int MAXIMUM_POOL_SIZE = 8;
    private static final long CONNECTION_TIMEOUT_MILLIS = 5_000;
    private static final DatabaseConnection INSTANCE = new DatabaseConnection(AppConfig.load());

    private final HikariDataSource dataSource;

    DatabaseConnection(AppConfig config) {
        AppConfig requiredConfig = Objects.requireNonNull(config, "config");
        HikariConfig poolConfig = new HikariConfig();
        poolConfig.setJdbcUrl(requiredConfig.databaseUrl());
        poolConfig.setUsername(requiredConfig.databaseUsername());
        poolConfig.setPassword(requiredConfig.databasePassword());
        poolConfig.setPoolName("event-portal-db");
        poolConfig.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        poolConfig.setMinimumIdle(0);
        poolConfig.setConnectionTimeout(CONNECTION_TIMEOUT_MILLIS);
        poolConfig.setInitializationFailTimeout(-1);
        dataSource = new HikariDataSource(poolConfig);
    }

    public static DatabaseConnection getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
