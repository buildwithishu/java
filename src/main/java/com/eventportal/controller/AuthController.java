package com.eventportal.controller;

import com.eventportal.dao.UserDAO;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.PasswordHasher;
import com.eventportal.util.SessionManager;
import com.eventportal.util.ValidationUtil;

import java.sql.SQLException;
import java.time.OffsetDateTime;
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

        Optional<User> user = userDAO.findByUsername(username.trim());
        if (user.isEmpty() || !PasswordHasher.verify(password, user.get().passwordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        sessionManager.login(user.get());
        return user.get();
    }

    public User registerStudent(String username, String email, String password, String fullName, String department) throws SQLException {
        validateRegistration(username, email, password, fullName);

        if (userDAO.findByUsername(username.trim()).isPresent()) {
            throw new IllegalArgumentException("Username is already registered");
        }
        if (userDAO.findByEmail(email.trim()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User newUser = new User(
                0,
                username.trim(),
                email.trim().toLowerCase(),
                PasswordHasher.hash(password),
                Role.STUDENT,
                fullName.trim(),
                normalizeOptionalText(department),
                OffsetDateTime.now()
        );

        return userDAO.createUser(newUser);
    }

    public void logout() {
        sessionManager.logout();
    }

    private static void validateRegistration(String username, String email, String password, String fullName) {
        if (ValidationUtil.isBlank(username)) {
            throw new IllegalArgumentException("Username is required");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("A valid email is required");
        }
        if (!ValidationUtil.isStrongPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters and include letters and digits");
        }
        if (ValidationUtil.isBlank(fullName)) {
            throw new IllegalArgumentException("Full name is required");
        }
    }

    private static String normalizeOptionalText(String value) {
        return ValidationUtil.isBlank(value) ? null : value.trim();
    }
}
