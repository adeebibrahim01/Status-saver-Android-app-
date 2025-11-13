package com.mariaxcodexpert.whatsdownloadplus.ui.tracker;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mariaxcodexpert.whatsdownloadplus.R;

import java.util.List;

public class KeywordAdapter extends RecyclerView.Adapter<KeywordAdapter.ViewHolder> {

    final List<String> keywords;
    final OnKeywordAction listener;

    public interface OnKeywordAction {
        void onSelect(String keyword);
        void onEdit(int position, String updated);
        void onDelete(int position);
        void onReorder(List<String> newOrder);
    }

    public KeywordAdapter(List<String> keywords, OnKeywordAction listener) {
        this.keywords = keywords;
        this.listener = listener;
    }

    @NonNull
    @Override
    public KeywordAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_keyword, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull KeywordAdapter.ViewHolder holder, int position) {
        String kw = keywords.get(position);
        holder.txtKeyword.setText(kw);

        holder.itemView.setOnClickListener(v -> listener.onSelect(kw));

        holder.itemView.setOnLongClickListener(v -> {
            showEditDialog(v.getContext(), position, kw);
            return true;
        });
    }

    private void showEditDialog(Context ctx, int position, String old) {
        EditText input = new EditText(ctx);
        input.setText(old);
        new AlertDialog.Builder(ctx)
                .setTitle("Edit keyword")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String updated = input.getText().toString().trim();
                    if (!updated.isEmpty()) {
                        keywords.set(position, updated);
                        listener.onEdit(position, updated);
                        notifyItemChanged(position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", (d, w) -> {
                    listener.onDelete(position);
                    notifyItemRemoved(position);
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return keywords.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtKeyword;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtKeyword = itemView.findViewById(R.id.txtKeyword);
        }
    }
}
