package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications10below;

import android.app.AlertDialog;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mariaxcodexpert.whatsdownloadplus.R;

import java.util.List;

public class NotificationAdapter10below extends RecyclerView.Adapter<NotificationAdapter10below.ViewHolder> {

    private List<NotificationModel10below> list;

    public NotificationAdapter10below(List<NotificationModel10below> list) {
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
        NotificationModel10below model = list.get(position);

        holder.sender.setText(model.getSender());
        holder.message.setText(model.getMessage());
        holder.time.setText(DateFormat.format("hh:mm a • dd MMM", model.getTimestamp()));

        holder.itemView.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(v.getContext())
                    .setTitle(model.getSender())
                    .setMessage(model.getMessage())
                    .setPositiveButton("Close", (d, which) -> d.dismiss())
                    .create();
            dialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void updateList(List<NotificationModel10below> newList) {
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
