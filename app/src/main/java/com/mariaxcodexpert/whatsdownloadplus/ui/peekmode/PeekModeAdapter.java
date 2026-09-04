package com.mariaxcodexpert.whatsdownloadplus.ui.peekmode;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.PeekMessageEntity.PeekMessageEntity;
import java.util.List;

public class PeekModeAdapter extends RecyclerView.Adapter<PeekModeAdapter.ViewHolder> {

    private List<PeekMessageEntity> messageList;

    public PeekModeAdapter(List<PeekMessageEntity> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_peek_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PeekMessageEntity msg = messageList.get(position);

        holder.txtName.setText(msg.senderName);

        String body = (msg.messageBody != null) ? msg.messageBody.toLowerCase() : "";
        if (body.contains(".jpg") || body.contains(".png") || body.contains(".mp4") || body.contains("photo") || body.contains("video")) {
            holder.txtMessage.setText("📷 Media Content");
            holder.txtMessage.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.txtMessage.setText(msg.messageBody != null ? msg.messageBody : "");
            holder.txtMessage.setTextColor(Color.GRAY);
        }

        holder.txtTime.setText(msg.timestamp);

        String name = (msg.senderName != null && !msg.senderName.isEmpty()) ? msg.senderName : "?";
        String firstLetter = String.valueOf(name.charAt(0)).toUpperCase();

        holder.txtAlphabet.setText(firstLetter);
        holder.txtAlphabet.setVisibility(View.VISIBLE);
        holder.txtAlphabet.setTextColor(Color.WHITE);
        holder.txtAlphabet.setBackground(getCircleDrawable(Color.parseColor("#9E9E9E")));

        if (msg.totalUnreadSum > 0) {
            holder.txtBadge.setVisibility(View.VISIBLE);
            String countText = (msg.totalUnreadSum > 99) ? "99+" : String.valueOf(msg.totalUnreadSum);
            holder.txtBadge.setText(countText);
        } else {
            holder.txtBadge.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), PeekChatDetailActivity.class);
            intent.putExtra("senderName", msg.senderName);
            v.getContext().startActivity(intent);
        });
    }

    private GradientDrawable getCircleDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    public void updateData(List<PeekMessageEntity> newMessages) {
        this.messageList = newMessages;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtMessage, txtTime, txtBadge, txtAlphabet;
        public ViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtBadge = itemView.findViewById(R.id.txtBadge);
            txtAlphabet = itemView.findViewById(R.id.txtAlphabet);
        }
    }
}