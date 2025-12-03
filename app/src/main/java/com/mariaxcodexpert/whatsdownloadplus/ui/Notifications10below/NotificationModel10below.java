package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications10below;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationModel10below {

    private final long id;                    // Database ID (-1 if not in DB yet)
    private final String sender;              // Sender name or number
    private String message;                   // Latest message text (or display text for group)
    private final long timestamp;             // Timestamp of the latest message
    private List<NotificationModel10below> groupedMessages; // All messages in the same chat
    private String chatId;                    // Unique chat identifier (group or single)

    // ------------------ Constructors ------------------

    public NotificationModel10below(long id, String sender, String message, long timestamp, String chatId) {
        this.id = id;
        this.sender = sender != null ? sender : "Unknown";
        this.message = message != null ? message : "";
        this.timestamp = timestamp;
        this.chatId = chatId != null ? chatId : this.sender;
        this.groupedMessages = new ArrayList<>();
    }

    public NotificationModel10below(String sender, String message, long timestamp, String chatId) {
        this(-1, sender, message, timestamp, chatId);
    }

    // ------------------ Getters ------------------

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

    public List<NotificationModel10below> getGroupedMessages() {
        if (groupedMessages == null) groupedMessages = new ArrayList<>();
        return groupedMessages;
    }

    public String getChatId() {
        return chatId;
    }

    // ------------------ Setters ------------------

    public void setMessage(String message) {
        if (message != null) this.message = message;
    }

    public void setGroupedMessages(List<NotificationModel10below> groupedMessages) {
        if (groupedMessages != null) this.groupedMessages = groupedMessages;
        else this.groupedMessages = new ArrayList<>();
    }

    public void addGroupedMessage(NotificationModel10below message) {
        if (message == null) return;
        if (this.groupedMessages == null) this.groupedMessages = new ArrayList<>();
        this.groupedMessages.add(0, message); // newest on top
    }

    public void setChatId(String chatId) {
        if (chatId != null) this.chatId = chatId;
    }

    // ------------------ Utility ------------------

    /** Returns number of grouped messages */
    public int getGroupedCount() {
        return groupedMessages != null ? groupedMessages.size() : 0;
    }

    /**
     * Returns the first/original message for preview in notification list
     */
    public String getPreviewMessage() {
        if (groupedMessages != null && !groupedMessages.isEmpty()) {
            // Last item = first/original message
            NotificationModel10below first = groupedMessages.get(groupedMessages.size() - 1);
            return first.getMessage() != null ? first.getMessage() : "";
        }
        return message != null ? message : "";
    }

    /**
     * Returns all messages in chronological order (oldest first)
     */
    public List<NotificationModel10below> getChronologicalMessages() {
        if (groupedMessages == null) return new ArrayList<>();
        List<NotificationModel10below> copy = new ArrayList<>(groupedMessages);
        Collections.reverse(copy); // oldest first
        return copy;
    }
}
