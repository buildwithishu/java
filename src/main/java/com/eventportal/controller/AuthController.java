package com.eventportal.controller;

import com.eventportal.dao.UserDAO;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.PasswordHasher;
import com.eventportal.util.SessionManager;
import com.eventportal.util.ValidationUtil;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

public class AuthController {
    private final UserDAO userDAO;
    private final SessionManager sessionManager;

    public AuthController(UserDAO userDAO, SessionManager sessionManager) {
        this.userDAO = userDAO;
        this.sessionManager = sessionManager;
    }

    public User login(String username, String password) throws SQLException {
        if (ValidationUtil.isBlank(username) || ValidationUtil.isBlank(password)) {
            throw new IllegalArgumentException("Username and password are required");
        }
        if (!ValidationUtil.isValidUsername(username.trim()) || !ValidationUtil.hasMaxLength(password, 72)) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        Optional<User> user = userDAO.findByUsername(username.trim());
        if (user.isEmpty() || !PasswordHasher.verify(password, user.get().passwordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        sessionManager.login(user.get());
        return user.get();
    }

    public User registerStudent(String username, String email, String password, String fullName, String department) throws SQLException {
        String normalizedUsername = normalizeRequiredText(username);
        String normalizedEmail = normalizeRequiredText(email).toLowerCase(Locale.ROOT);
        String normalizedFullName = normalizeRequiredText(fullName);
        String normalizedDepartment = normalizeOptionalText(department);
        validateRegistration(normalizedUsername, normalizedEmail, password, normalizedFullName);
        validateDepartment(normalizedDepartment);

        if (userDAO.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("Username is already registered");
        }
        if (userDAO.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User newUser = new User(
                0,
                normalizedUsername,
                normalizedEmail,
                PasswordHasher.hash(password),
                Role.STUDENT,
                normalizedFullName,
                normalizedDepartment,
                OffsetDateTime.now()
        );

        return userDAO.createUser(newUser);
    }

    public void logout() {
        sessionManager.logout();
    }

    private static void validateRegistration(String username, String email, String password, String fullName) {
        if (!ValidationUtil.isValidUsername(username)) {
            throw new IllegalArgumentException("Username must be 3-50 letters, digits, dots, underscores, or hyphens");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("A valid email of at most 120 characters is required");
        }
        if (!ValidationUtil.isStrongPassword(password)) {
            throw new IllegalArgumentException("Password must be 8-72 characters and include letters and digits");
        }
        if (ValidationUtil.isBlank(fullName)) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (!ValidationUtil.hasMaxLength(fullName, 120)) {
            throw new IllegalArgumentException("Full name must be at most 120 characters");
        }
    }

    private static void validateDepartment(String department) {
        if (department != null && !ValidationUtil.hasMaxLength(department, 120)) {
            throw new IllegalArgumentException("Department must be at most 120 characters");
        }
    }

    private static String normalizeOptionalText(String value) {
        return ValidationUtil.isBlank(value) ? null : value.trim();
    }

    private static String normalizeRequiredText(String value) {
        return value == null ? "" : value.trim();
    }
}
