package com.digiq.model;

import java.io.Serializable;

/** The headline numbers on the admin dashboard tiles. */
public class DashboardStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private int issuedToday;
    private int completedToday;
    private int waitingNow;
    private int inServiceNow;
    private int noShowToday;
    private int openCounters;
    private int totalCounters;
    private int totalCustomers;
    private int totalStaff;
    private int totalServices;
    private double avgWaitMinutes;
    private double avgServiceMinutes;

    public int getIssuedToday() {
        return issuedToday;
    }

    public void setIssuedToday(int issuedToday) {
        this.issuedToday = issuedToday;
    }

    public int getCompletedToday() {
        return completedToday;
    }

    public void setCompletedToday(int completedToday) {
        this.completedToday = completedToday;
    }

    public int getWaitingNow() {
        return waitingNow;
    }

    public void setWaitingNow(int waitingNow) {
        this.waitingNow = waitingNow;
    }

    public int getInServiceNow() {
        return inServiceNow;
    }

    public void setInServiceNow(int inServiceNow) {
        this.inServiceNow = inServiceNow;
    }

    public int getNoShowToday() {
        return noShowToday;
    }

    public void setNoShowToday(int noShowToday) {
        this.noShowToday = noShowToday;
    }

    public int getOpenCounters() {
        return openCounters;
    }

    public void setOpenCounters(int openCounters) {
        this.openCounters = openCounters;
    }

    public int getTotalCounters() {
        return totalCounters;
    }

    public void setTotalCounters(int totalCounters) {
        this.totalCounters = totalCounters;
    }

    public int getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(int totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public int getTotalStaff() {
        return totalStaff;
    }

    public void setTotalStaff(int totalStaff) {
        this.totalStaff = totalStaff;
    }

    public int getTotalServices() {
        return totalServices;
    }

    public void setTotalServices(int totalServices) {
        this.totalServices = totalServices;
    }

    public double getAvgWaitMinutes() {
        return avgWaitMinutes;
    }

    public void setAvgWaitMinutes(double avgWaitMinutes) {
        this.avgWaitMinutes = avgWaitMinutes;
    }

    public double getAvgServiceMinutes() {
        return avgServiceMinutes;
    }

    public void setAvgServiceMinutes(double avgServiceMinutes) {
        this.avgServiceMinutes = avgServiceMinutes;
    }

    /** Share of today's issued tokens that reached COMPLETED, as a whole percentage. */
    public int getCompletionRate() {
        if (issuedToday == 0) {
            return 0;
        }
        return (int) Math.round(completedToday * 100.0 / issuedToday);
    }
}
