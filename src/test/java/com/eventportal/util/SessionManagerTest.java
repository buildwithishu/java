package com.eventportal.util;

import com.eventportal.model.Role;
import com.eventportal.model.User;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SessionManagerTest {
    @Test
    void loginAndLogout_shouldManageCurrentUser() {
        SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.logout();

        User user = new User(
                1,
                "student",
                "student@college.edu",
                "hash",
                Role.STUDENT,
                "Student User",
                "Computer Science",
                OffsetDateTime.now()
        );

        sessionManager.login(user);

        assertThat(sessionManager.isLoggedIn()).isTrue();
        assertThat(sessionManager.getCurrentUser()).contains(user);

        sessionManager.logout();

        assertThat(sessionManager.isLoggedIn()).isFalse();
        assertThat(sessionManager.getCurrentUser()).isEmpty();
    }
}
