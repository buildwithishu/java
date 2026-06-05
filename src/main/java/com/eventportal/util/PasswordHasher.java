package com.eventportal.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public final class PasswordHasher {
    private static final int COST = 12;

    private PasswordHasher() {
    }

    public static String hash(String password) {
        if (!ValidationUtil.isStrongPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters and include letters and digits");
        }
        return BCrypt.withDefaults().hashToString(COST, password.toCharArray());
    }

    public static boolean verify(String password, String passwordHash) {
        if (password == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        return BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified;
    }
}
