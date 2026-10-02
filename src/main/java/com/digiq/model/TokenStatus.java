package com.digiq.model;

/**
 * Where a token is in its life.
 *
 * <pre>
 *   PENDING --(staff calls next)--&gt; IN_SERVICE --(staff completes)--&gt; COMPLETED
 *      |                                 |
 *      |                                 +--(customer never showed)--&gt; NO_SHOW
 *      +--(customer cancels)--&gt; CANCELLED
 * </pre>
 *
 * <p>Each constant carries its own display label and a "tone", which is the name
 * of a colour role in the stylesheet. That lets a JSP write
 * {@code class="badge badge--${token.status.tone}"} and get the right colour
 * without a single if-statement in the view.</p>
 */
public enum TokenStatus {

    // Booked and waiting in the queue. The only state a customer may cancel from.
    PENDING("Waiting", "warning"),

    // Called to a counter and being served right now.
    IN_SERVICE("In Service", "info"),

    // Served and finished. Terminal.
    COMPLETED("Completed", "good"),

    // Given up by the customer before being called. Terminal.
    CANCELLED("Cancelled", "muted"),

    // Called, but the customer never presented. Terminal.
    NO_SHOW("No Show", "critical");

    // Shown on badges and in tables.
    private final String label;

    // Maps onto the status colour tokens in digiq.css (good / warning / critical / info / muted).
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

    /**
     * True while the token still occupies a place in the queue.
     *
     * <p>The three terminal states are all "done" as far as the queue is concerned,
     * so grouping them here avoids repeating the same two-way comparison.</p>
     */
    public boolean isActive() {
        return this == PENDING || this == IN_SERVICE;
    }

    /**
     * Converts the database string into a constant.
     *
     * <p>Defaults to PENDING, the harmless starting state, rather than throwing.</p>
     */
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
