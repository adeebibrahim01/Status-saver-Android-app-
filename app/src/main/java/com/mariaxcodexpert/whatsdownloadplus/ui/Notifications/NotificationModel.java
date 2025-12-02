package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications;

import java.util.List;

public class NotificationModel {
    private final long id; // 🔹 add id
    private final String sender;
    private String message;
    private final long timestamp;
    private List<NotificationModel> groupedMessages;

    public NotificationModel(long id, String sender, String message, long timestamp) {
        this.id = id;
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Constructor for grouped notifications without individual id
    public NotificationModel(String sender, String message, long timestamp) {
        this.id = -1; // placeholder for grouped messages
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setGroupedMessages(List<NotificationModel> messages) {
        this.groupedMessages = messages;
    }

    public List<NotificationModel> getGroupedMessages() {
        return groupedMessages;
    }

    public void setMessage(String s) {
        this.message = s;
    }
}
