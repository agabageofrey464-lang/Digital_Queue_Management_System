package com.digiq.model;

/**
 * The three kinds of account in the system.
 *
 * <p>An enum rather than a plain string column wrapper, so an invalid role cannot
 * exist in Java at all. Each constant also carries the two things the rest of the
 * application keeps needing: a human-readable label for the screen, and the page
 * that role is sent to after signing in.</p>
 */
public enum Role {

    // Full control: configures services, counters and accounts, and reads analytics.
    ADMIN("Administrator", "/admin/dashboard"),

    // Works one counter: calls, scans and completes tokens.
    STAFF("Counter Staff", "/staff/console"),

    // Books tokens and tracks their own place in the queue.
    CUSTOMER("Customer", "/customer/home");

    // What the user sees, e.g. in the sidebar and the Users table.
    private final String label;

    // Where this role lands immediately after a successful sign-in.
    private final String landingPage;

    /** Enum constructors are always private; called once per constant above. */
    Role(String label, String landingPage) {
        this.label = label;
        this.landingPage = landingPage;
    }

    /** Used by the JSPs as ${user.role.label}. */
    public String getLabel() {
        return label;
    }

    /**
     * Where this role is sent immediately after a successful login.
     *
     * <p>Keeping it on the enum means the login servlet never needs a switch
     * statement, and adding a role cannot leave it with nowhere to go.</p>
     */
    public String getLandingPage() {
        return landingPage;
    }

    /**
     * Converts the database string into a constant.
     *
     * <p>Falls back to the least privileged role rather than throwing. A row with
     * an unreadable role should degrade to "can do almost nothing", never to a
     * 500 page and never to something more powerful.</p>
     */
    public static Role from(String raw) {
        // Null column, or no value submitted on a form.
        if (raw == null) {
            return CUSTOMER;
        }
        try {
            // Tolerates surrounding spaces and any casing, e.g. " admin ".
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            // The string did not match any constant - fail closed.
            return CUSTOMER;
        }
    }
}
