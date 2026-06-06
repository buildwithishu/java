package com.eventportal;

import com.eventportal.dao.DatabaseConnection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AppTest {
    @Test
    void appName_shouldDescribePortal() {
        assertThat(App.APP_NAME).isEqualTo("College Event Management Portal");
    }

    @Test
    void stop_shouldCloseDatabasePool() {
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);

        new App(databaseConnection).stop();

        verify(databaseConnection).close();
    }
}
