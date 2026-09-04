package com.mariaxcodexpert.whatsdownloadplus.ui.support;

import androidx.annotation.Keep;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Keep
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 5L;

    public String messageId = "";
    public String message = "";
    public String sender = "user";
    public String role = "";
    public String timestamp = "";
    public boolean seen = false;
    public long serverTimestamp = 0L;

    public ChatMessage() {
        this.messageId = "";
        this.message = "";
        this.sender = "user";
        this.role = "";
        this.timestamp = "";
        this.serverTimestamp = System.currentTimeMillis();
    }

    public ChatMessage(String messageId, String message, String role, String timestamp, boolean seen, long serverTimestamp) {
        this.messageId = (messageId != null) ? messageId : "";
        this.message = (message != null) ? message : "";
        this.role = (role != null) ? role : "";
        this.timestamp = (timestamp != null) ? timestamp : "";
        this.seen = seen;
        this.serverTimestamp = (serverTimestamp != 0) ? serverTimestamp : System.currentTimeMillis();
        this.sender = ("auto_admin".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role)) ? "admin" : "user";
    }

    public boolean isAdminMessage() {
        return "admin".equalsIgnoreCase(this.sender) || (role != null && role.toLowerCase().contains("admin"));
    }

    public String getDisplayTime() {
        if (this.serverTimestamp <= 0) return "";

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(this.serverTimestamp));
    }

    public boolean isValid() {
        return message != null && !message.trim().isEmpty();
    }
}