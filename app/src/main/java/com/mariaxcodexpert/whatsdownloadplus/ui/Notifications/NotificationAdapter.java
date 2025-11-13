package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications;

import android.app.AlertDialog;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.whatsapp.WhatsAppMessageAdapter;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationModel> list;

    public NotificationAdapter(List<NotificationModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel model = list.get(position);

        holder.sender.setText(model.getSender());
        holder.message.setText(model.getMessage());
        holder.time.setText(DateFormat.format("hh:mm a • dd MMM", model.getTimestamp()));

        holder.itemView.setOnClickListener(v -> {
            if (model.getGroupedMessages() != null && model.getGroupedMessages().size() > 1) {
                // Inflate custom WhatsApp-style dialog
                View dialogView = LayoutInflater.from(v.getContext())
                        .inflate(R.layout.dialog_whatsapp_style, null);

                TextView dialogSender = dialogView.findViewById(R.id.dialogSender);
                RecyclerView dialogRecycler = dialogView.findViewById(R.id.dialogRecyclerView);
                View closeButton = dialogView.findViewById(R.id.dialogCloseButton);

                dialogSender.setText(model.getSender() + " (" + model.getGroupedMessages().size() + " messages)");

                WhatsAppMessageAdapter msgAdapter = new WhatsAppMessageAdapter(model.getGroupedMessages());
                dialogRecycler.setLayoutManager(new LinearLayoutManager(v.getContext()));
                dialogRecycler.setAdapter(msgAdapter);

                AlertDialog dialog = new AlertDialog.Builder(v.getContext())
                        .setView(dialogView)
                        .setCancelable(true)
                        .create();

                closeButton.setOnClickListener(b -> dialog.dismiss());

                dialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void updateList(List<NotificationModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView sender, message, time;

        ViewHolder(View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.senderText);
            message = itemView.findViewById(R.id.messageText);
            time = itemView.findViewById(R.id.timeText);
        }
    }
}