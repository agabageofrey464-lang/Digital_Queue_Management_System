// Declares the package this class belongs to.
package com.digiq.model;

// Lets the object be stored in an HTTP session if ever needed.
import java.io.Serializable;
// The SQL timestamp type returned by the created_at column.
import java.sql.Timestamp;

/**
 * A physical service point, staffed by one member of counter staff.
 *
 * <p>Each counter is bound to exactly one service, which is what makes "call the
 * next customer" unambiguous: the counter already knows which queue it drains.</p>
 */
// Serializable for consistency with the other beans.
public class Counter implements Serializable {

    // Fixes the serialization version across future field changes.
    private static final long serialVersionUID = 1L;

    // Primary key from the counters table.
    private int id;
    // What is printed on the desk and shown on the board, e.g. "Counter 1".
    private String name;
    // Which service this counter handles. Not nullable - a counter with no service
    // could never pull from a queue.
    private int serviceId;
    // Which staff account operates it. Nullable, because a counter can exist before
    // anybody is assigned to it.
    private Integer staffId;
    // OPEN, PAUSED or CLOSED. Defaults to CLOSED so a newly created counter never
    // starts taking customers before somebody opens it.
    private CounterStatus status = CounterStatus.CLOSED;
    // When the row was inserted, set by the database default.
    private Timestamp createdAt;

    // ------------------------------------------------------------------
    // Denormalised for display. Resolved by joins in CounterDAO, not columns here.
    // ------------------------------------------------------------------

    // The service's display name, e.g. "Account Opening".
    private String serviceName;
    // The service's token prefix, e.g. ACC.
    private String serviceCode;
    // The assigned staff member's name, or null when unassigned.
    private String staffName;
    // The token being served right now, or null when the counter is idle.
    private String currentTokenNumber;
    // How many tokens this counter has completed today.
    private int servedToday;

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

    /** Returns the counter's display name. */
    public String getName() {
        // Hand back the stored name.
        return name;
    }

    /** Sets the counter's display name. */
    public void setName(String name) {
        // Copy the argument onto the field.
        this.name = name;
    }

    /** Returns the id of the service this counter handles. */
    public int getServiceId() {
        // Hand back the stored service id.
        return serviceId;
    }

    /** Sets the service this counter handles. */
    public void setServiceId(int serviceId) {
        // Copy the argument onto the field.
        this.serviceId = serviceId;
    }

    /** Returns the assigned staff id, or null when unassigned. */
    public Integer getStaffId() {
        // Hand back the stored staff id. Integer, not int, so it can be null.
        return staffId;
    }

    /** Assigns a staff member, or null to leave the counter unstaffed. */
    public void setStaffId(Integer staffId) {
        // Copy the argument onto the field.
        this.staffId = staffId;
    }

    /** Returns whether the counter is open, paused or closed. */
    public CounterStatus getStatus() {
        // Hand back the stored status.
        return status;
    }

    /** Sets the counter's status. */
    public void setStatus(CounterStatus status) {
        // Copy the argument onto the field.
        this.status = status;
    }

    /** Returns when the counter was created. */
    public Timestamp getCreatedAt() {
        // Hand back the stored timestamp.
        return createdAt;
    }

    /** Sets the creation timestamp, read from the database. */
    public void setCreatedAt(Timestamp createdAt) {
        // Copy the argument onto the field.
        this.createdAt = createdAt;
    }

    /** Returns the joined-in service name. */
    public String getServiceName() {
        // Hand back the joined-in name.
        return serviceName;
    }

    /** Sets the joined-in service name. */
    public void setServiceName(String serviceName) {
        // Copy the argument onto the field.
        this.serviceName = serviceName;
    }

    /** Returns the joined-in service code. */
    public String getServiceCode() {
        // Hand back the joined-in code.
        return serviceCode;
    }

    /** Sets the joined-in service code. */
    public void setServiceCode(String serviceCode) {
        // Copy the argument onto the field.
        this.serviceCode = serviceCode;
    }

    /** Returns the joined-in staff name, or null when unassigned. */
    public String getStaffName() {
        // Hand back the joined-in name.
        return staffName;
    }

    /** Sets the joined-in staff name. */
    public void setStaffName(String staffName) {
        // Copy the argument onto the field.
        this.staffName = staffName;
    }

    /** Returns the token being served now, or null when idle. */
    public String getCurrentTokenNumber() {
        // Hand back the joined-in token number.
        return currentTokenNumber;
    }

    /** Sets the token currently being served. */
    public void setCurrentTokenNumber(String currentTokenNumber) {
        // Copy the argument onto the field.
        this.currentTokenNumber = currentTokenNumber;
    }

    /** Returns how many tokens this counter completed today. */
    public int getServedToday() {
        // Hand back the joined-in count.
        return servedToday;
    }

    /** Sets today's completed count. */
    public void setServedToday(int servedToday) {
        // Copy the argument onto the field.
        this.servedToday = servedToday;
    }

    /**
     * True when somebody is at the desk right now.
     *
     * <p>Derived from the current token rather than stored, so it cannot disagree
     * with what the board is showing.</p>
     */
    public boolean isBusy() {
        // A non-null current token is exactly what "busy" means here.
        return currentTokenNumber != null;
    }
}
