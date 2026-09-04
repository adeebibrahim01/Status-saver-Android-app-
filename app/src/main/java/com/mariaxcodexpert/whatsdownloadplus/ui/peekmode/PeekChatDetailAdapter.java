package com.mariaxcodexpert.whatsdownloadplus.ui.peekmode;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.PeekMessageEntity.PeekMessageEntity;

import java.util.List;

public class PeekChatDetailAdapter extends RecyclerView.Adapter<PeekChatDetailAdapter.ViewHolder> {

    private List<PeekMessageEntity> messageList;

    public PeekChatDetailAdapter(List<PeekMessageEntity> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_peek_chat_detail_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PeekMessageEntity msg = messageList.get(position);
        holder.txtMessage.setText(msg.messageBody);
        holder.txtTime.setText(msg.timestamp);
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage, txtTime;
        public ViewHolder(View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
        }
    }
}