package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.Download.FullScreenMediaActivity;

import java.util.List;

public class RecentDownloadsAdapter extends RecyclerView.Adapter<RecentDownloadsAdapter.ViewHolder> {

    private final Context context;
    private final List<MediaItem> items;

    public RecentDownloadsAdapter(Context context, List<MediaItem> items) {
        this.context = context;
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb, videoIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgThumb);
            videoIcon = itemView.findViewById(R.id.videoIcon); // may be null if layout missing it
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_download_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItem item = items.get(position);

        // Load thumbnail with Glide
        Glide.with(context)
                .load(item.uri)
                .centerCrop()
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .into(holder.imgThumb);

        // Show video icon if available
        if (holder.videoIcon != null) {
            holder.videoIcon.setVisibility(item.isVideo ? View.VISIBLE : View.GONE);
        } else {
            // Optional: debug toast
            Toast.makeText(context, "videoIcon ImageView is missing in layout!", Toast.LENGTH_SHORT).show();
        }

        // Click listener: open full screen media
        holder.imgThumb.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullScreenMediaActivity.class);
            intent.putExtra(FullScreenMediaActivity.EXTRA_URI, item.uri);
            intent.putExtra(FullScreenMediaActivity.EXTRA_IS_VIDEO, item.isVideo);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
