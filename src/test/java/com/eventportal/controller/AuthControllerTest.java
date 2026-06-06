package com.eventportal.controller;

import com.eventportal.dao.UserDAO;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.PasswordHasher;
import com.eventportal.util.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock
    private UserDAO userDAO;

    private final SessionManager sessionManager = SessionManager.getInstance();
    private AuthController authController;

    @BeforeEach
    void setUp() {
        sessionManager.logout();
        authController = new AuthController(userDAO, sessionManager);
    }

    @AfterEach
    void tearDown() {
        sessionManager.logout();
    }

    @Test
    void login_shouldCreateSessionForValidCredentials() throws SQLException {
        User user = user(7, "student", "student@college.edu", PasswordHasher.hash("secret123"));
        when(userDAO.findByUsername("student")).thenReturn(Optional.of(user));

        User loggedIn = authController.login(" student ", "secret123");

        assertThat(loggedIn).isEqualTo(user);
        assertThat(sessionManager.getCurrentUser()).contains(user);
    }

    @Test
    void login_shouldRejectInvalidPasswordWithoutCreatingSession() throws SQLException {
        User user = user(7, "student", "student@college.edu", PasswordHasher.hash("secret123"));
        when(userDAO.findByUsername("student")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authController.login("student", "wrong-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password");
        assertThat(sessionManager.isLoggedIn()).isFalse();
    }

    @Test
    void registerStudent_shouldNormalizeAndHashInput() throws SQLException {
        when(userDAO.findByUsername("newstudent")).thenReturn(Optional.empty());
        when(userDAO.findByEmail("student@college.edu")).thenReturn(Optional.empty());
        when(userDAO.createUser(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User created = authController.registerStudent(
                " newstudent ",
                " Student@College.edu ",
                "secure123",
                " New Student ",
                " Computer Science "
        );

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDAO).createUser(captor.capture());
        User persisted = captor.getValue();
        assertThat(created).isEqualTo(persisted);
        assertThat(persisted.username()).isEqualTo("newstudent");
        assertThat(persisted.email()).isEqualTo("student@college.edu");
        assertThat(persisted.fullName()).isEqualTo("New Student");
        assertThat(persisted.department()).isEqualTo("Computer Science");
        assertThat(persisted.role()).isEqualTo(Role.STUDENT);
        assertThat(PasswordHasher.verify("secure123", persisted.passwordHash())).isTrue();
    }

    @Test
    void registerStudent_shouldRejectDuplicateUsernameBeforeWriting() throws SQLException {
        when(userDAO.findByUsername("student")).thenReturn(Optional.of(
                user(3, "student", "existing@college.edu", "hash")
        ));

        assertThatThrownBy(() -> authController.registerStudent(
                "student",
                "new@college.edu",
                "secure123",
                "New Student",
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is already registered");
        verify(userDAO, never()).createUser(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void registerStudent_shouldRejectInvalidInputBeforeDatabaseAccess() {
        assertThatThrownBy(() -> authController.registerStudent(
                "student",
                "not-an-email",
                "weak",
                "Student",
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A valid email is required");
        verifyNoInteractions(userDAO);
    }

    private static User user(long id, String username, String email, String passwordHash) {
        return new User(
                id,
                username,
                email,
                passwordHash,
                Role.STUDENT,
                "Student User",
                "Computer Science",
                OffsetDateTime.now()
        );
    }
}
