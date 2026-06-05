package com.eventportal.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AppConfig {
    private static final String CONFIG_RESOURCE = "/config.properties";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("^\\$\\{([A-Z0-9_]+):(.+)}$");

    private final Properties properties;
    private final Map<String, String> environment;

    private AppConfig(Properties properties, Map<String, String> environment) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    public static AppConfig load() {
        Properties loadedProperties = new Properties();

        try (InputStream inputStream = AppConfig.class.getResourceAsStream(CONFIG_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing config resource: " + CONFIG_RESOURCE);
            }
            loadedProperties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load config resource: " + CONFIG_RESOURCE, exception);
        }

        return new AppConfig(loadedProperties, System.getenv());
    }

    static AppConfig from(Properties properties, Map<String, String> environment) {
        return new AppConfig(properties, environment);
    }

    public String getRequired(String key) {
        String rawValue = properties.getProperty(key);
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Missing required config key: " + key);
        }
        return resolveValue(rawValue);
    }

    public String databaseUrl() {
        return getRequired("db.url");
    }

    public String databaseUsername() {
        return getRequired("db.username");
    }

    public String databasePassword() {
        return getRequired("db.password");
    }

    public String certificatesDirectory() {
        return getRequired("certificates.dir");
    }

    private String resolveValue(String rawValue) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(rawValue.trim());
        if (!matcher.matches()) {
            return rawValue;
        }

        String envKey = matcher.group(1);
        String defaultValue = matcher.group(2);
        String envValue = environment.get(envKey);
        return envValue == null || envValue.isBlank() ? defaultValue : envValue;
    }
}
