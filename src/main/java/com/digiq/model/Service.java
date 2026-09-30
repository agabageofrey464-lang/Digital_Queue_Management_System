package com.digiq.model;

import java.io.Serializable;
import java.sql.Timestamp;

/** A service a customer can queue for, e.g. "Account Opening" (code ACC). */
public class Service implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String code;
    private String description;
    private int avgServiceMinutes = 10;
    private boolean active = true;
    private Timestamp createdAt;

    /* Populated by joins on the queue screens - not columns of `services`. */
    private int waitingCount;
    private int openCounters;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getAvgServiceMinutes() {
        return avgServiceMinutes;
    }

    public void setAvgServiceMinutes(int avgServiceMinutes) {
        this.avgServiceMinutes = avgServiceMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getWaitingCount() {
        return waitingCount;
    }

    public void setWaitingCount(int waitingCount) {
        this.waitingCount = waitingCount;
    }

    public int getOpenCounters() {
        return openCounters;
    }

    public void setOpenCounters(int openCounters) {
        this.openCounters = openCounters;
    }

    /**
     * Rough wait for somebody joining this queue right now: the people ahead,
     * divided across the counters actually serving, times the average handling time.
     */
    public int getEstimatedWaitMinutes() {
        int lanes = Math.max(openCounters, 1);
        return (int) Math.ceil((double) waitingCount / lanes) * avgServiceMinutes;
    }
}
