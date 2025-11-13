package com.mariaxcodexpert.whatsdownloadplus.whatsapp;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.Notifications.NotificationModel;

import java.util.List;

public class WhatsAppMessageAdapter extends RecyclerView.Adapter<WhatsAppMessageAdapter.ViewHolder> {

    private final List<NotificationModel> messages;

    public WhatsAppMessageAdapter(List<NotificationModel> messages) {
        this.messages = messages;
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
        NotificationModel msg = messages.get(position);
        holder.messageText.setText(msg.getMessage());
        holder.timeText.setText(DateFormat.format("hh:mm a", msg.getTimestamp()));
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
