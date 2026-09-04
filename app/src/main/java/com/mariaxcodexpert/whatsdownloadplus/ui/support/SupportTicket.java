package com.mariaxcodexpert.whatsdownloadplus.ui.support;

import androidx.annotation.Keep;
import java.io.Serializable;


@Keep
public class SupportTicket implements Serializable {

    private static final long serialVersionUID = 4L;

    public String ticketId = "";
    public String subject = "Support Request";
    public String status = "Open"; // Default: Open
    public String lastMessage = "";
    public String timestamp = "";
    public boolean hasNotification = false;

    public SupportTicket() {
        this.ticketId = "";
        this.subject = "Support Request";
        this.status = "Open";
        this.lastMessage = "";
        this.timestamp = "";
    }

    public SupportTicket(String ticketId, String subject, String status, String lastMessage, String timestamp) {
        this.ticketId = (ticketId != null) ? ticketId : "";
        this.subject = (subject != null) ? subject : "Support Request";
        this.status = (status != null) ? status : "Open";
        this.lastMessage = (lastMessage != null) ? lastMessage : "";
        this.timestamp = (timestamp != null) ? timestamp : "";
    }
    public String getFormattedDate() {
        return (timestamp != null && !timestamp.isEmpty()) ? timestamp : "N/A";
    }
}