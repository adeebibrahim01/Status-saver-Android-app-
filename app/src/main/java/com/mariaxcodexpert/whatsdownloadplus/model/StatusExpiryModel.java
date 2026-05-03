package com.mariaxcodexpert.whatsdownloadplus.model;

public class StatusExpiryModel {
    public String token;
    public int statusId;
    public long expiryTime;

    public StatusExpiryModel() { } // Firebase ke liye zaroori hai

    public StatusExpiryModel(String token, int statusId, long expiryTime) {
        this.token = token;
        this.statusId = statusId;
        this.expiryTime = expiryTime;
    }
}