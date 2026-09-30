package com.digiq.model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

/** One place in a queue, identified by a printable number and a scannable UUID. */
public class Token implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String tokenNumber;
    private String qrPayload;
    private int serviceId;
    private int customerId;
    private Integer counterId;
    private TokenStatus status = TokenStatus.PENDING;
    private boolean priority;
    private Date serviceDate;
    private Timestamp issuedAt;
    private Timestamp calledAt;
    private Timestamp completedAt;

    /* Denormalised for display. */
    private String serviceName;
    private String serviceCode;
    private String customerName;
    private String customerPhone;
    private String counterName;
    private int avgServiceMinutes = 10;

    /** How many still-waiting tokens sit ahead of this one. Filled by the queue service. */
    private int positionInQueue = -1;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public Integer getCounterId() {
        return counterId;
    }

    public void setCounterId(Integer counterId) {
        this.counterId = counterId;
    }

    public TokenStatus getStatus() {
        return status;
    }

    public void setStatus(TokenStatus status) {
        this.status = status;
    }

    public boolean isPriority() {
        return priority;
    }

    public void setPriority(boolean priority) {
        this.priority = priority;
    }

    public Date getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(Date serviceDate) {
        this.serviceDate = serviceDate;
    }

    public Timestamp getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Timestamp issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Timestamp getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(Timestamp calledAt) {
        this.calledAt = calledAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public int getAvgServiceMinutes() {
        return avgServiceMinutes;
    }

    public void setAvgServiceMinutes(int avgServiceMinutes) {
        this.avgServiceMinutes = avgServiceMinutes;
    }

    public int getPositionInQueue() {
        return positionInQueue;
    }

    public void setPositionInQueue(int positionInQueue) {
        this.positionInQueue = positionInQueue;
    }

    /** Minutes the customer waited before being called (0 once called). */
    public long getWaitMinutes() {
        if (issuedAt == null || calledAt == null) {
            return 0;
        }
        return (calledAt.getTime() - issuedAt.getTime()) / 60000L;
    }

    /** Minutes spent at the counter. */
    public long getServiceMinutes() {
        if (calledAt == null || completedAt == null) {
            return 0;
        }
        return (completedAt.getTime() - calledAt.getTime()) / 60000L;
    }

    /** Best-guess minutes until this token is called. */
    public int getEstimatedWaitMinutes() {
        if (positionInQueue <= 0) {
            return 0;
        }
        return positionInQueue * avgServiceMinutes;
    }
}
