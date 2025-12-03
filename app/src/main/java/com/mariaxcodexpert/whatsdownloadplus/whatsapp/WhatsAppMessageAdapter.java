package com.mariaxcodexpert.whatsdownloadplus.whatsapp;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.Notifications.NotificationModel10Above;
import com.mariaxcodexpert.whatsdownloadplus.ui.Notifications10below.NotificationModel10below;

import java.util.List;

public class WhatsAppMessageAdapter extends RecyclerView.Adapter<WhatsAppMessageAdapter.ViewHolder> {

    private final List<?> messages;  // ← ANY list (below or above)
    private final boolean isBelow;   // ← Tells which model to use

    public WhatsAppMessageAdapter(List<?> messages, boolean isBelow) {
        this.messages = messages;
        this.isBelow = isBelow;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_whatsapp_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (isBelow) {
            // Android < 10 model
            NotificationModel10below msg = (NotificationModel10below) messages.get(position);
            holder.messageText.setText(msg.getMessage());
            holder.timeText.setText(DateFormat.format("hh:mm a", msg.getTimestamp()));
        } else {
            // Android ≥ 10 model
            NotificationModel10Above msg = (NotificationModel10Above) messages.get(position);
            holder.messageText.setText(msg.getMessage());
            holder.timeText.setText(DateFormat.format("hh:mm a", msg.getTimestamp()));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        ViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
        }
    }
}
