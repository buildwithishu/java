package com.eventportal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {
    @Test
    void appName_shouldDescribePortal() {
        assertThat(App.APP_NAME).isEqualTo("College Event Management Portal");
    }
}
