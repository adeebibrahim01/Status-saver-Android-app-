// File: StatusItem.java
package com.mariaxcodexpert.whatsdownloadplus.model;

public class StatusItem {
    private long expiryTime;          // exact expiry timestamp
    private boolean notificationSent; // whether 1-hour notification has been sent
    private String name;              // optional: status file name

    public StatusItem(long expiryTime, String name) {
        this.expiryTime = expiryTime;
        this.name = name;
        this.notificationSent = false;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public String getName() {
        return name;
    }
}
