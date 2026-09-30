package com.digiq.model;

/** Whether a counter is accepting work right now. */
public enum CounterStatus {
    OPEN("Open", "good"),
    PAUSED("Paused", "warning"),
    CLOSED("Closed", "muted");

    private final String label;
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

    /** Only an OPEN counter may pull the next token off the queue. */
    public boolean canServe() {
        return this == OPEN;
    }

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
