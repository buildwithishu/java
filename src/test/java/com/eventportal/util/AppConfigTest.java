package com.eventportal.util;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppConfigTest {
    @Test
    void getRequired_shouldUseDefaultWhenEnvironmentValueMissing() {
        Properties properties = new Properties();
        properties.setProperty("db.url", "${APP_DB_URL:jdbc:postgresql://localhost:5432/event_portal_db}");

        AppConfig config = AppConfig.from(properties, Map.of());

        assertThat(config.getRequired("db.url")).isEqualTo("jdbc:postgresql://localhost:5432/event_portal_db");
    }

    @Test
    void getRequired_shouldUseEnvironmentValueWhenPresent() {
        Properties properties = new Properties();
        properties.setProperty("db.url", "${APP_DB_URL:jdbc:postgresql://localhost:55432/event_portal_test_db}");

        AppConfig config = AppConfig.from(properties, Map.of("APP_DB_URL", "jdbc:postgresql://db:5432/custom"));

        assertThat(config.getRequired("db.url")).isEqualTo("jdbc:postgresql://db:5432/custom");
    }

    @Test
    void getRequired_shouldRejectMissingKey() {
        AppConfig config = AppConfig.from(new Properties(), Map.of());

        assertThatThrownBy(() -> config.getRequired("db.url"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("db.url");
    }
}
