package com.mariaxcodexpert.whatsdownloadplus.ui.tracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class KeywordAdapter extends RecyclerView.Adapter<KeywordAdapter.ViewHolder> {

    private final List<String> keywords;
    private final OnKeywordClickListener listener;

    public interface OnKeywordClickListener {
        void onKeywordClick(String keyword);
    }

    public KeywordAdapter(List<String> keywords, OnKeywordClickListener listener) {
        this.keywords = keywords;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String keyword = keywords.get(position);
        holder.textView.setText(keyword);
        holder.textView.setOnClickListener(v -> listener.onKeywordClick(keyword));
    }

    @Override
    public int getItemCount() {
        return keywords.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
