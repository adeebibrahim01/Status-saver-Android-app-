package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ImagesAndVideoAdapter extends RecyclerView.Adapter<ImagesAndVideoAdapter.GalleryViewHolder> {

    private final Context context;
    private boolean isVideo;
    private List<MediaItem> mediaItems;

    private final OnDownloadClickListener downloadListener;
    private final OnItemClickListener itemClickListener;

    private final List<GalleryViewHolder> visibleHolders = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public interface OnDownloadClickListener { void onDownload(DocumentFile file); }
    public interface OnItemClickListener { void onItemClick(DocumentFile file); }

    // Model class to store DocumentFile and its expiryTime
    public static class MediaItem {
        public final DocumentFile file;
        public final long expiryTime;

        public MediaItem(DocumentFile file) {
            this.file = file;
            this.expiryTime = file.lastModified() + 24 * 60 * 60 * 1000; // 24 hours
        }
    }

    public ImagesAndVideoAdapter(Context context,
                                 List<DocumentFile> mediaList,
                                 boolean isVideo,
                                 OnDownloadClickListener downloadListener,
                                 OnItemClickListener itemClickListener) {
        this.context = context;
        this.downloadListener = downloadListener;
        this.itemClickListener = itemClickListener;
        this.mediaItems = new ArrayList<>();
        for (DocumentFile f : mediaList) mediaItems.add(new MediaItem(f));
        this.isVideo = isVideo;

        // Start background countdown updates
        scheduler.scheduleAtFixedRate(this::updateCountdowns, 0, 1, TimeUnit.SECONDS);
    }

    // Update adapter data
    public void updateData(List<DocumentFile> files, boolean isVideo) {
        mediaItems = new ArrayList<>();
        for (DocumentFile f : files) mediaItems.add(new MediaItem(f));
        this.isVideo = isVideo;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_images_videos, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        MediaItem item = mediaItems.get(position);
        DocumentFile file = item.file;
        Uri uri = file.getUri();

        Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .centerCrop()
                .into(holder.imageThumb);

        boolean isSaved = isFileSavedInStatusSaver(file.getName(), file.getType());
        holder.downloadIcon.setVisibility(isSaved ? View.GONE : View.VISIBLE);
        holder.downloadStatus.setVisibility(isSaved ? View.VISIBLE : View.GONE);
        holder.videoIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        holder.downloadIcon.setOnClickListener(v -> {
            if (downloadListener != null) {
                downloadListener.onDownload(file);
                holder.downloadIcon.setVisibility(View.GONE);
                holder.downloadStatus.setVisibility(View.VISIBLE);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) itemClickListener.onItemClick(file);
            Intent intent = new Intent(context, ImageVideoPreviewActivity.class);
            intent.putExtra(ImageVideoPreviewActivity.EXTRA_URI, file.getUri());
            boolean isVideoFile = file.getName() != null &&
                    file.getName().toLowerCase().matches(".*\\.(mp4|mkv|3gp)$");
            intent.putExtra(ImageVideoPreviewActivity.EXTRA_IS_VIDEO, isVideoFile);
            context.startActivity(intent);
        });

        // **Use expiryTime from MediaItem model**
        holder.expiryTime = item.expiryTime;

        if (!visibleHolders.contains(holder)) visibleHolders.add(holder);
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    private boolean isFileSavedInStatusSaver(String fileName, String mimeType) {
        if (fileName == null) return false;
        Uri collection;
        String selection;
        String[] selectionArgs;

        if (mimeType != null && mimeType.startsWith("video")) {
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            selection = MediaStore.Video.Media.DISPLAY_NAME + "=? AND " +
                    MediaStore.Video.Media.RELATIVE_PATH + " LIKE ?";
            selectionArgs = new String[]{fileName, "%Status Saver%"};
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            selection = MediaStore.Images.Media.DISPLAY_NAME + "=? AND " +
                    MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
            selectionArgs = new String[]{fileName, "%Status Saver%"};
        }

        try (Cursor cursor = context.getContentResolver().query(
                collection, new String[]{MediaStore.MediaColumns._ID}, selection, selectionArgs, null)) {
            return cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Update countdowns in main thread */
    private void updateCountdowns() {
        long now = System.currentTimeMillis();
        for (GalleryViewHolder holder : visibleHolders) {
            long remaining = holder.expiryTime - now;
            holder.countdownTimer.post(() -> {
                if (remaining <= 0) {
                    holder.countdownTimer.setText("Expired");
                    holder.countdownTimer.setTextColor(0xFFFF0000);
                } else {
                    long hours = TimeUnit.MILLISECONDS.toHours(remaining);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;
                    holder.countdownTimer.setText(String.format("Expires in %02d:%02d:%02d", hours, minutes, seconds));

                    if (hours < 1) holder.countdownTimer.setTextColor(0xFFFF0000);
                    else if (hours < 6) holder.countdownTimer.setTextColor(0xFFFFA500);
                    else holder.countdownTimer.setTextColor(0xFF00FF00);
                }
            });
        }
    }

    public static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb, downloadIcon, videoIcon, downloadStatus;
        TextView countdownTimer;
        long expiryTime;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            downloadIcon = itemView.findViewById(R.id.downloadIcon);
            downloadStatus = itemView.findViewById(R.id.downloadStatus);
            videoIcon = itemView.findViewById(R.id.videoIcon);
            countdownTimer = itemView.findViewById(R.id.countdownTimer);
        }
    }

    @Override
    public void onViewRecycled(@NonNull GalleryViewHolder holder) {
        super.onViewRecycled(holder);
        visibleHolders.remove(holder);
    }

    /** Shutdown scheduler when adapter is no longer used */
    public void shutdownScheduler() {
        scheduler.shutdownNow();
    }
}
