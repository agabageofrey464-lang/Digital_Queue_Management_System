// Declares the package this class belongs to.
package com.digiq.model;

// Lets the object be stored in an HTTP session or cached if ever needed.
import java.io.Serializable;
// The SQL timestamp type returned by the created_at column.
import java.sql.Timestamp;

/**
 * A service a customer can queue for, e.g. "Account Opening" with the code ACC.
 *
 * <p>The last two fields are not columns. They are filled in by the queue-aware
 * query so a booking card can show how busy a service is without the view running
 * its own counts.</p>
 */
// Serializable for the same reason as the other beans.
public class Service implements Serializable {

    // Fixes the serialization version across future field changes.
    private static final long serialVersionUID = 1L;

    // Primary key from the services table. 0 means "not saved yet".
    private int id;
    // What the customer sees on the booking card, e.g. "Account Opening".
    private String name;
    // The token prefix, e.g. ACC, which produces token numbers like ACC-0042.
    private String code;
    // One line explaining the service, shown under its name.
    private String description;
    // How long one customer typically takes at the counter, in minutes. Drives the
    // estimated wait. Defaults to 10 so a half-built object still computes sensibly.
    private int avgServiceMinutes = 10;
    // Whether customers may currently book this. Retired services stay in the table
    // so their historical tokens keep a valid foreign key.
    private boolean active = true;
    // When the row was inserted, set by the database default.
    private Timestamp createdAt;

    // ------------------------------------------------------------------
    // Populated by joins on the queue screens - NOT columns of `services`.
    // ------------------------------------------------------------------

    // How many tokens are still PENDING for this service today.
    private int waitingCount;
    // How many counters are currently OPEN for this service.
    private int openCounters;

    /** Returns the primary key. */
    public int getId() {
        // Hand back the stored id.
        return id;
    }

    /** Sets the primary key. */
    public void setId(int id) {
        // Copy the argument onto the field.
        this.id = id;
    }

    /** Returns the display name. */
    public String getName() {
        // Hand back the stored name.
        return name;
    }

    /** Sets the display name. */
    public void setName(String name) {
        // Copy the argument onto the field.
        this.name = name;
    }

    /** Returns the token prefix, e.g. ACC. */
    public String getCode() {
        // Hand back the stored code.
        return code;
    }

    /** Sets the token prefix. Callers upper-case it before saving. */
    public void setCode(String code) {
        // Copy the argument onto the field.
        this.code = code;
    }

    /** Returns the one-line description, which may be null. */
    public String getDescription() {
        // Hand back the stored description.
        return description;
    }

    /** Sets the one-line description. */
    public void setDescription(String description) {
        // Copy the argument onto the field.
        this.description = description;
    }

    /** Returns the typical handling time in minutes. */
    public int getAvgServiceMinutes() {
        // Hand back the stored average.
        return avgServiceMinutes;
    }

    /** Sets the typical handling time. Callers clamp it to at least 1. */
    public void setAvgServiceMinutes(int avgServiceMinutes) {
        // Copy the argument onto the field.
        this.avgServiceMinutes = avgServiceMinutes;
    }

    /** True when customers may book this service. */
    public boolean isActive() {
        // Hand back the stored flag.
        return active;
    }

    /** Enables or retires the service. */
    public void setActive(boolean active) {
        // Copy the argument onto the field.
        this.active = active;
    }

    /** Returns when the service was created. */
    public Timestamp getCreatedAt() {
        // Hand back the stored timestamp.
        return createdAt;
    }

    /** Sets the creation timestamp, read from the database. */
    public void setCreatedAt(Timestamp createdAt) {
        // Copy the argument onto the field.
        this.createdAt = createdAt;
    }

    /** Returns how many customers are waiting for this service today. */
    public int getWaitingCount() {
        // Hand back the joined-in count.
        return waitingCount;
    }

    /** Sets the waiting count, filled in by the queue-aware query. */
    public void setWaitingCount(int waitingCount) {
        // Copy the argument onto the field.
        this.waitingCount = waitingCount;
    }

    /** Returns how many counters are currently serving this service. */
    public int getOpenCounters() {
        // Hand back the joined-in count.
        return openCounters;
    }

    /** Sets the open-counter count, filled in by the queue-aware query. */
    public void setOpenCounters(int openCounters) {
        // Copy the argument onto the field.
        this.openCounters = openCounters;
    }

    /**
     * Rough wait, in minutes, for somebody joining this queue right now.
     *
     * <p>People ahead, shared across the counters actually serving, multiplied by the
     * average handling time. Computed rather than stored, so it is always current.</p>
     */
    public int getEstimatedWaitMinutes() {
        // Treat a closed service as having one lane rather than zero, which would
        // divide by zero below. The number is then a worst case, which is the honest
        // answer when nobody is serving.
        int lanes = Math.max(openCounters, 1);
        // Round UP: four people across three counters is two rounds of service, not
        // one and a third, so ceiling is the realistic figure.
        return (int) Math.ceil((double) waitingCount / lanes) * avgServiceMinutes;
    }
}
