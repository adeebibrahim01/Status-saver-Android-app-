package com.mariaxcodexpert.whatsdownloadplus.model;

import androidx.annotation.Keep;
import com.google.firebase.database.PropertyName;

@Keep
public class StatusExpiryModel {

    public String token;
    public String identity;
    public int statusId;
    public String expiryTime;
    public boolean isNotified;

    public StatusExpiryModel() {}

    public StatusExpiryModel(String token, String identity, int statusId, String expiryTime, boolean isNotified) {
        this.token = token;
        this.identity = identity;
        this.statusId = statusId;
        this.expiryTime = expiryTime;
        this.isNotified = isNotified;
    }
}