package com.eventportal.util;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public final class TestAppConfig {
    private TestAppConfig() {
    }

    public static AppConfig withCertificatesDirectory(Path directory) {
        Properties properties = new Properties();
        properties.setProperty("certificates.dir", directory.toString());
        return AppConfig.from(properties, Map.of());
    }

    public static AppConfig withDatabase(String url, String username, String password) {
        Properties properties = new Properties();
        properties.setProperty("db.url", url);
        properties.setProperty("db.username", username);
        properties.setProperty("db.password", password);
        return AppConfig.from(properties, Map.of());
    }
}
