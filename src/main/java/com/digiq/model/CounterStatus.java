package com.digiq.model;

/**
 * Whether a counter is accepting work at this moment.
 *
 * <p>Separate from whether a staff member is assigned: a counter can have somebody
 * behind it and still be PAUSED while they deal with something, which is why this
 * is a state on the counter rather than something inferred from the staff column.</p>
 */
public enum CounterStatus {

    // Serving customers, and may pull the next token off the queue.
    OPEN("Open", "good"),

    // Temporarily not calling anyone - on a break, or finishing paperwork.
    // Still shown on the public board so the waiting room knows it exists.
    PAUSED("Paused", "warning"),

    // Not in use at all today.
    CLOSED("Closed", "muted");

    // Shown on badges and on the display board.
    private final String label;

    // Colour role in the stylesheet, same idea as TokenStatus.
    private final String tone;

    CounterStatus(String label, String tone) {
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
     * Whether this counter may call the next customer.
     *
     * <p>Deliberately a method rather than a comparison scattered through the
     * servlets: if "paused counters may finish their current customer but not call
     * a new one" ever needs changing, it changes in exactly one place.</p>
     */
    public boolean canServe() {
        return this == OPEN;
    }

    /**
     * Converts the database string into a constant.
     *
     * <p>Defaults to CLOSED - the safe answer, since a counter whose state cannot
     * be read must not start pulling customers out of the queue.</p>
     */
    public static CounterStatus from(String raw) {
        if (raw == null) {
            return CLOSED;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CLOSED;
        }
    }
}
