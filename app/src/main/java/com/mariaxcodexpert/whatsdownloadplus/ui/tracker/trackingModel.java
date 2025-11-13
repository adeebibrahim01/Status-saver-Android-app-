package com.mariaxcodexpert.whatsdownloadplus.ui.tracker;

import java.util.List;

public class trackingModel {
    private final long id; // Unique ID for each notification
    private final String sender;
    private final String message;
    private final long timestamp;
    private List<trackingModel> groupedMessages;

    // Constructor with id
    public trackingModel(long id, String sender, String message, long timestamp) {
        this.id = id;
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Constructor for grouped notifications (no id)
    public trackingModel(String sender, String message, long timestamp) {
        this.id = -1;
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }

    public void setGroupedMessages(List<trackingModel> messages) { this.groupedMessages = messages; }
    public List<trackingModel> getGroupedMessages() { return groupedMessages; }
}
