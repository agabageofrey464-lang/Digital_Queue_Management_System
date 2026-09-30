package com.digiq.model;

/**
 * Token lifecycle.
 *
 * <pre>
 *   PENDING --(staff calls next)--> IN_SERVICE --(staff completes)--> COMPLETED
 *      |                                 |
 *      |                                 +--(customer never showed)--> NO_SHOW
 *      +--(customer cancels)--> CANCELLED
 * </pre>
 */
public enum TokenStatus {
    PENDING("Waiting", "warning"),
    IN_SERVICE("In Service", "info"),
    COMPLETED("Completed", "good"),
    CANCELLED("Cancelled", "muted"),
    NO_SHOW("No Show", "critical");

    private final String label;
    /** Maps onto the status colour tokens in the stylesheet. */
    private final String tone;

    TokenStatus(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() {
        return label;
    }

    public String getTone() {
        return tone;
    }

    /** A token that is still occupying a place in the queue. */
    public boolean isActive() {
        return this == PENDING || this == IN_SERVICE;
    }

    public static TokenStatus from(String raw) {
        if (raw == null) {
            return PENDING;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PENDING;
        }
    }
}
