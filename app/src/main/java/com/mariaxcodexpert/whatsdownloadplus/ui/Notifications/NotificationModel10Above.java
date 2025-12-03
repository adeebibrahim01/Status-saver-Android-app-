package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications;

import java.util.List;

public class NotificationModel10Above {
    private final long id; // 🔹 add id
    private final String sender;
    private final String message;
    private final long timestamp;
    private List<NotificationModel10Above> groupedMessages;

    public NotificationModel10Above(long id, String sender, String message, long timestamp) {
        this.id = id;
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Constructor for grouped notifications without individual id
    public NotificationModel10Above(String sender, String message, long timestamp) {
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

    public void setGroupedMessages(List<NotificationModel10Above> messages) {
        this.groupedMessages = messages;
    }

    public List<NotificationModel10Above> getGroupedMessages() {
        return groupedMessages;
    }
}