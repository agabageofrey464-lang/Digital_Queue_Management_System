package com.digiq.util;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt hashing for account passwords. */
public final class PasswordUtil {

    private static final int COST = 10;

    private PasswordUtil() {
    }

    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(COST));
    }

    /**
     * Verifies a password against a stored hash.
     *
     * <p>jBCrypt 0.4 only recognises the {@code $2a$} prefix, but hashes produced by
     * other tooling are commonly tagged {@code $2b$} or {@code $2y$}. Those revisions
     * differ only in a bug fix that does not affect passwords under 256 bytes, so the
     * prefix is normalised rather than rejected - otherwise a perfectly valid hash
     * seeded from outside Java would fail to log in.</p>
     */
    public static boolean matches(String plain, String hash) {
        if (plain == null || hash == null || hash.length() < 4) {
            return false;
        }
        String normalised = hash;
        if (hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
            normalised = "$2a$" + hash.substring(4);
        }
        try {
            return BCrypt.checkpw(plain, normalised);
        } catch (IllegalArgumentException ex) {
            // Malformed hash in the database - treat as a failed login, never a 500.
            return false;
        }
    }
}
