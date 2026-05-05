package com.mariaxcodexpert.whatsdownloadplus.model;

public class StatusExpiryModel {
    public String token;
    public String deviceId;
    public int statusId;
    public String expiryTime; // String format for UTC readability
    public boolean notified;

    public StatusExpiryModel() { } // Firebase requirements

    public StatusExpiryModel(String token, String deviceId, int statusId, String expiryTime, boolean notified) {
        this.token = token;
        this.deviceId = deviceId;
        this.statusId = statusId;
        this.expiryTime = expiryTime;
        this.notified = notified;
    }
}