package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;
import java.util.ArrayList;

public class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.TrendViewHolder> {

    private ArrayList<TrendingModel> trendList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onSetStatusClick(TrendingModel model);
    }

    public TrendingAdapter(ArrayList<TrendingModel> trendList, OnItemClickListener listener) {
        this.trendList = trendList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending, parent, false);
        return new TrendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrendViewHolder holder, int position) {
        TrendingModel currentItem = trendList.get(position);

        holder.tvTitle.setText(currentItem.getTitle());

        // Glide use karke thumbnail load karna
        Glide.with(holder.itemView.getContext())
                .load(currentItem.getThumbnailUrl())
                .placeholder(R.drawable.placeholder_image)
                .centerCrop()
                .into(holder.ivThumbnail);

        holder.btnSetStatus.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetStatusClick(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return trendList.size();
    }

    public static class TrendViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivThumbnail;
        public TextView tvTitle;
        public Button btnSetStatus;

        public TrendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvTitle = itemView.findViewById(R.id.tvTrendTitle);
            btnSetStatus = itemView.findViewById(R.id.btnSetStatus);
        }
    }
}