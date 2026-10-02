// Declares the package this class belongs to.
package com.digiq.model;

// Lets the object be stored in an HTTP session if ever needed.
import java.io.Serializable;
// The SQL timestamp type returned by the created_at column.
import java.sql.Timestamp;

/**
 * One alert raised for one customer about one of their tokens.
 *
 * <p>Rows are written by {@code QueueService} at each step of the queue, and read
 * back by the customer's notifications page. They are never deleted, so the feed
 * doubles as a per-customer history of what they were told and when.</p>
 */
// Serializable for consistency with the other beans.
public class Notification implements Serializable {

    // Fixes the serialization version across future field changes.
    private static final long serialVersionUID = 1L;

    // Primary key from the notifications table.
    private int id;
    // Who the alert is for. Every query is scoped by this, so one customer can
    // never read another's feed.
    private int userId;
    // Which token it concerns. Nullable, because a general announcement has no token.
    private Integer tokenId;
    // The bold line, e.g. "It is your turn".
    private String title;
    // The explanatory sentence under the title.
    private String message;
    // INFO, APPROACHING, CALLED or COMPLETED. Chooses the icon on the feed, and is
    // what stops the "almost your turn" alert being raised more than once per token.
    private String type = "INFO";
    // Whether the customer has seen it. Drives the sidebar badge count.
    private boolean read;
    // When the alert was raised, set by the database default.
    private Timestamp createdAt;

    // Joined in from the tokens table for display, so the feed can show "ACC-0042"
    // without the view having to look the token up itself.
    private String tokenNumber;

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

    /** Returns the id of the customer this alert belongs to. */
    public int getUserId() {
        // Hand back the stored user id.
        return userId;
    }

    /** Sets the owning customer. */
    public void setUserId(int userId) {
        // Copy the argument onto the field.
        this.userId = userId;
    }

    /** Returns the related token id, or null for a general alert. */
    public Integer getTokenId() {
        // Hand back the stored token id. Integer, not int, so it can be null.
        return tokenId;
    }

    /** Sets the related token id, or null. */
    public void setTokenId(Integer tokenId) {
        // Copy the argument onto the field.
        this.tokenId = tokenId;
    }

    /** Returns the bold headline of the alert. */
    public String getTitle() {
        // Hand back the stored title.
        return title;
    }

    /** Sets the headline. */
    public void setTitle(String title) {
        // Copy the argument onto the field.
        this.title = title;
    }

    /** Returns the explanatory sentence. */
    public String getMessage() {
        // Hand back the stored message.
        return message;
    }

    /** Sets the explanatory sentence. */
    public void setMessage(String message) {
        // Copy the argument onto the field.
        this.message = message;
    }

    /** Returns the alert type, which selects the icon. */
    public String getType() {
        // Hand back the stored type.
        return type;
    }

    /** Sets the alert type. */
    public void setType(String type) {
        // Copy the argument onto the field.
        this.type = type;
    }

    /** True once the customer has seen this alert. */
    public boolean isRead() {
        // Hand back the stored flag.
        return read;
    }

    /** Marks the alert as read or unread. */
    public void setRead(boolean read) {
        // Copy the argument onto the field.
        this.read = read;
    }

    /** Returns when the alert was raised. */
    public Timestamp getCreatedAt() {
        // Hand back the stored timestamp.
        return createdAt;
    }

    /** Sets the creation timestamp, read from the database. */
    public void setCreatedAt(Timestamp createdAt) {
        // Copy the argument onto the field.
        this.createdAt = createdAt;
    }

    /** Returns the related token number, joined in for display. */
    public String getTokenNumber() {
        // Hand back the joined-in token number.
        return tokenNumber;
    }

    /** Sets the joined-in token number. */
    public void setTokenNumber(String tokenNumber) {
        // Copy the argument onto the field.
        this.tokenNumber = tokenNumber;
    }
}
