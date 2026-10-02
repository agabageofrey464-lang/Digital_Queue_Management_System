// Declares the package this class belongs to.
package com.digiq.model;

// Lets the object be stored in an HTTP session if ever needed.
import java.io.Serializable;
// The SQL timestamp type returned by the created_at column.
import java.sql.Timestamp;

/**
 * One immutable row of the audit trail.
 *
 * <p>Written inside the same transaction as the status change it records, so the
 * log cannot drift from the tokens table even if the request fails halfway. There
 * is deliberately no update or delete path anywhere for these rows.</p>
 */
// Serializable for consistency with the other beans.
public class ServiceLog implements Serializable {

    // Fixes the serialization version across future field changes.
    private static final long serialVersionUID = 1L;

    // Primary key from the service_logs table.
    private int id;
    // Which token the entry describes.
    private int tokenId;
    // Which counter was involved. Nullable - booking and cancelling have no counter.
    private Integer counterId;
    // Which staff member acted. Nullable - the customer's own actions have no staff.
    private Integer staffId;
    // What happened: ISSUED, CALLED, COMPLETED, NO_SHOW or CANCELLED.
    private String action;
    // The status before the change. Null for the very first entry of a token's life.
    private String fromStatus;
    // The status after the change.
    private String toStatus;
    // Optional free text, e.g. why a token was closed out.
    private String note;
    // When it happened, set by the database default.
    private Timestamp createdAt;

    // ------------------------------------------------------------------
    // Denormalised for display. Resolved by joins in ServiceLogDAO.
    // ------------------------------------------------------------------

    // The token's printable number, e.g. ACC-0042.
    private String tokenNumber;
    // The counter's name, or null.
    private String counterName;
    // The acting staff member's name, or null.
    private String staffName;
    // The service the token belonged to.
    private String serviceName;

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

    /** Returns the token this entry describes. */
    public int getTokenId() {
        // Hand back the stored token id.
        return tokenId;
    }

    /** Sets the token this entry describes. */
    public void setTokenId(int tokenId) {
        // Copy the argument onto the field.
        this.tokenId = tokenId;
    }

    /** Returns the counter involved, or null. */
    public Integer getCounterId() {
        // Hand back the stored counter id. Integer so it can be null.
        return counterId;
    }

    /** Sets the counter involved, or null. */
    public void setCounterId(Integer counterId) {
        // Copy the argument onto the field.
        this.counterId = counterId;
    }

    /** Returns the staff member who acted, or null. */
    public Integer getStaffId() {
        // Hand back the stored staff id. Integer so it can be null.
        return staffId;
    }

    /** Sets the staff member who acted, or null. */
    public void setStaffId(Integer staffId) {
        // Copy the argument onto the field.
        this.staffId = staffId;
    }

    /** Returns what happened, e.g. CALLED. */
    public String getAction() {
        // Hand back the stored action.
        return action;
    }

    /** Sets what happened. */
    public void setAction(String action) {
        // Copy the argument onto the field.
        this.action = action;
    }

    /** Returns the status before the change, or null for the first entry. */
    public String getFromStatus() {
        // Hand back the stored previous status.
        return fromStatus;
    }

    /** Sets the status before the change. */
    public void setFromStatus(String fromStatus) {
        // Copy the argument onto the field.
        this.fromStatus = fromStatus;
    }

    /** Returns the status after the change. */
    public String getToStatus() {
        // Hand back the stored resulting status.
        return toStatus;
    }

    /** Sets the status after the change. */
    public void setToStatus(String toStatus) {
        // Copy the argument onto the field.
        this.toStatus = toStatus;
    }

    /** Returns the optional free-text note. */
    public String getNote() {
        // Hand back the stored note.
        return note;
    }

    /** Sets the optional free-text note. */
    public void setNote(String note) {
        // Copy the argument onto the field.
        this.note = note;
    }

    /** Returns when the change happened. */
    public Timestamp getCreatedAt() {
        // Hand back the stored timestamp.
        return createdAt;
    }

    /** Sets the timestamp, read from the database. */
    public void setCreatedAt(Timestamp createdAt) {
        // Copy the argument onto the field.
        this.createdAt = createdAt;
    }

    /** Returns the joined-in token number. */
    public String getTokenNumber() {
        // Hand back the joined-in token number.
        return tokenNumber;
    }

    /** Sets the joined-in token number. */
    public void setTokenNumber(String tokenNumber) {
        // Copy the argument onto the field.
        this.tokenNumber = tokenNumber;
    }

    /** Returns the joined-in counter name. */
    public String getCounterName() {
        // Hand back the joined-in counter name.
        return counterName;
    }

    /** Sets the joined-in counter name. */
    public void setCounterName(String counterName) {
        // Copy the argument onto the field.
        this.counterName = counterName;
    }

    /** Returns the joined-in staff name. */
    public String getStaffName() {
        // Hand back the joined-in staff name.
        return staffName;
    }

    /** Sets the joined-in staff name. */
    public void setStaffName(String staffName) {
        // Copy the argument onto the field.
        this.staffName = staffName;
    }

    /** Returns the joined-in service name. */
    public String getServiceName() {
        // Hand back the joined-in service name.
        return serviceName;
    }

    /** Sets the joined-in service name. */
    public void setServiceName(String serviceName) {
        // Copy the argument onto the field.
        this.serviceName = serviceName;
    }
}
