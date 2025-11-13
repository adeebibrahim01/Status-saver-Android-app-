package com.mariaxcodexpert.whatsdownloadplus.ui.tracker;

import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mariaxcodexpert.whatsdownloadplus.R;

import java.util.List;

public class trackingAdapter extends RecyclerView.Adapter<trackingAdapter.Holder> {

    private List<trackingModel> list;
    private String highlightKeyword = "";

    public trackingAdapter(List<trackingModel> list) {
        this.list = list;
    }

    public void setHighlightKeyword(String keyword) {
        this.highlightKeyword = keyword != null ? keyword : "";
        notifyDataSetChanged();
    }

    public void updateList(List<trackingModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        trackingModel m = list.get(position);
        holder.sender.setText(m.getSender());

        String message = m.getMessage() != null ? m.getMessage() : "";

        if (!highlightKeyword.isEmpty() && message.toLowerCase().contains(highlightKeyword.toLowerCase())) {
            // Highlight all occurrences of keyword (case-insensitive)
            SpannableString spannable = new SpannableString(message);
            String lowerMessage = message.toLowerCase();
            String lowerKeyword = highlightKeyword.toLowerCase();
            int start = 0;

            while (start >= 0) {
                start = lowerMessage.indexOf(lowerKeyword, start);
                if (start >= 0) {
                    int end = start + highlightKeyword.length();
                    spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new ForegroundColorSpan(
                                    ContextCompat.getColor(holder.itemView.getContext(), R.color.teal_700)),
                            start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    start = end; // move to next
                }
            }
            holder.message.setText(spannable);
        } else {
            holder.message.setText(message);
        }

        holder.time.setText(android.text.format.DateFormat.format("hh:mm a • dd MMM", m.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView sender, message, time;

        Holder(@NonNull View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.senderText);
            message = itemView.findViewById(R.id.messageText);
            time = itemView.findViewById(R.id.timeText);
        }
    }
}
