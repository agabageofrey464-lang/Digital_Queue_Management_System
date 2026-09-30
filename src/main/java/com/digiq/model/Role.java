package com.digiq.model;

/** The three actors in the system. */
public enum Role {
    ADMIN("Administrator", "/admin/dashboard"),
    STAFF("Counter Staff", "/staff/console"),
    CUSTOMER("Customer", "/customer/home");

    private final String label;
    private final String landingPage;

    Role(String label, String landingPage) {
        this.label = label;
        this.landingPage = landingPage;
    }

    public String getLabel() {
        return label;
    }

    /** Where this role is sent immediately after a successful login. */
    public String getLandingPage() {
        return landingPage;
    }

    public static Role from(String raw) {
        if (raw == null) {
            return CUSTOMER;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CUSTOMER;
        }
    }
}
