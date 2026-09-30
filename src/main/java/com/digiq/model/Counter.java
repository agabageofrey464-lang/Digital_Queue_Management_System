package com.digiq.model;

import java.io.Serializable;
import java.sql.Timestamp;

/** A physical service point staffed by one member of counter staff. */
public class Counter implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private int serviceId;
    private Integer staffId;
    private CounterStatus status = CounterStatus.CLOSED;
    private Timestamp createdAt;

    /* Denormalised for display. */
    private String serviceName;
    private String serviceCode;
    private String staffName;
    private String currentTokenNumber;
    private int servedToday;

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

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public CounterStatus getStatus() {
        return status;
    }

    public void setStatus(CounterStatus status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getCurrentTokenNumber() {
        return currentTokenNumber;
    }

    public void setCurrentTokenNumber(String currentTokenNumber) {
        this.currentTokenNumber = currentTokenNumber;
    }

    public int getServedToday() {
        return servedToday;
    }

    public void setServedToday(int servedToday) {
        this.servedToday = servedToday;
    }

    public boolean isBusy() {
        return currentTokenNumber != null;
    }
}
