package com.eventportal.util;

import com.eventportal.model.User;

import java.util.Optional;

public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    private User currentUser;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public synchronized void login(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user is required");
        }
        currentUser = user;
    }

    public synchronized void logout() {
        currentUser = null;
    }

    public synchronized Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }
}
