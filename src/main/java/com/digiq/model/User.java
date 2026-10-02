// Declares the package this class belongs to.
package com.digiq.model;

// Marks the class as able to be written to a byte stream, which the servlet
// container needs in order to store it in an HTTP session.
import java.io.Serializable;
// The SQL timestamp type returned by the created_at column.
import java.sql.Timestamp;

/**
 * One account, of any of the three roles.
 *
 * <p>A plain Java bean: fields plus getters and setters, no behaviour beyond the
 * initials helper. JSP Expression Language reads properties through getters, so the
 * naming here is what makes {@code ${user.fullName}} work in a view.</p>
 */
// Serializable so this object can live in the session between requests.
public class User implements Serializable {

    // Fixes the serialization version, so a future change to the fields does not
    // break sessions that are already in flight.
    private static final long serialVersionUID = 1L;

    // Primary key from the users table. 0 means "not saved yet".
    private int id;
    // The person's display name, shown in the top bar and in tables.
    private String fullName;
    // The sign-in identifier. Unique in the database; may be a plain username.
    private String email;
    // Optional contact number, shown to staff when they scan a ticket.
    private String phone;
    // The BCrypt hash. Marked transient so it is NEVER written into a session or
    // serialised anywhere - the view layer must not be able to reach it.
    private transient String passwordHash;
    // Which area of the application this account may use. Defaults to the least
    // privileged role, so a half-built object can never be accidentally powerful.
    private Role role = Role.CUSTOMER;
    // Whether the account may sign in. Defaults to true for newly created users.
    private boolean active = true;
    // When the row was inserted, set by the database default.
    private Timestamp createdAt;

    /** Returns the primary key. */
    public int getId() {
        // Hand back the stored id.
        return id;
    }

    /** Sets the primary key, normally straight after an insert. */
    public void setId(int id) {
        // Copy the argument onto the field.
        this.id = id;
    }

    /** Returns the display name. */
    public String getFullName() {
        // Hand back the stored name.
        return fullName;
    }

    /** Sets the display name. */
    public void setFullName(String fullName) {
        // Copy the argument onto the field.
        this.fullName = fullName;
    }

    /** Returns the sign-in identifier. */
    public String getEmail() {
        // Hand back the stored email.
        return email;
    }

    /** Sets the sign-in identifier. Callers lower-case it before saving. */
    public void setEmail(String email) {
        // Copy the argument onto the field.
        this.email = email;
    }

    /** Returns the contact number, which may be null. */
    public String getPhone() {
        // Hand back the stored phone number.
        return phone;
    }

    /** Sets the contact number. */
    public void setPhone(String phone) {
        // Copy the argument onto the field.
        this.phone = phone;
    }

    /**
     * Returns the BCrypt hash.
     *
     * <p>Only the authentication path should call this. Everywhere else nulls it out
     * before the object travels any further.</p>
     */
    public String getPasswordHash() {
        // Hand back the stored hash.
        return passwordHash;
    }

    /** Stores the BCrypt hash. Never give this a plain-text password. */
    public void setPasswordHash(String passwordHash) {
        // Copy the argument onto the field.
        this.passwordHash = passwordHash;
    }

    /** Returns the role, which decides what this account may reach. */
    public Role getRole() {
        // Hand back the stored role.
        return role;
    }

    /** Sets the role. */
    public void setRole(Role role) {
        // Copy the argument onto the field.
        this.role = role;
    }

    /** True when the account is allowed to sign in. */
    public boolean isActive() {
        // Hand back the stored flag. Named isX so EL sees it as the "active" property.
        return active;
    }

    /** Enables or disables the account. */
    public void setActive(boolean active) {
        // Copy the argument onto the field.
        this.active = active;
    }

    /** Returns when the account was created. */
    public Timestamp getCreatedAt() {
        // Hand back the stored timestamp.
        return createdAt;
    }

    /** Sets the creation timestamp, read from the database. */
    public void setCreatedAt(Timestamp createdAt) {
        // Copy the argument onto the field.
        this.createdAt = createdAt;
    }

    /**
     * First and last initials, for the round avatar chip in the top bar.
     *
     * <p>Computed rather than stored, so it can never fall out of step with the name.</p>
     */
    public String getInitials() {
        // Guard against a missing or blank name, which would otherwise crash below.
        if (fullName == null || fullName.isBlank()) {
            // A single question mark is a readable placeholder in the avatar circle.
            return "?";
        }
        // Split on any run of whitespace, so double spaces do not produce empty parts.
        String[] parts = fullName.trim().split("\\s+");
        // Accumulates the one or two letters that make up the result.
        StringBuilder sb = new StringBuilder();
        // Always take the first letter of the first name, upper-cased.
        sb.append(Character.toUpperCase(parts[0].charAt(0)));
        // Only add a second letter when there is more than one word.
        if (parts.length > 1) {
            // Use the LAST word rather than the second, so a middle name is skipped.
            sb.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        }
        // Produce the finished one- or two-letter string.
        return sb.toString();
    }
}
