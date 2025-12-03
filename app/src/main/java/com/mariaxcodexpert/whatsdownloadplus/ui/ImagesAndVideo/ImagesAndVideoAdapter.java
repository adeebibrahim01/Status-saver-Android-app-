package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ImagesAndVideoAdapter extends RecyclerView.Adapter<ImagesAndVideoAdapter.GalleryViewHolder> {

    private final Context context;
    private boolean isVideo;
    private List<MediaItem> mediaItems;
    private final List<GalleryViewHolder> visibleHolders = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static class MediaItem {
        public final DocumentFile file;
        public final long expiryTime;

        public MediaItem(DocumentFile file) {
            this.file = file;
            this.expiryTime = file.lastModified() + 24 * 60 * 60 * 1000; // 24h expiry
        }
    }

    public ImagesAndVideoAdapter(Context context, List<DocumentFile> mediaList, boolean isVideo) {
        this.context = context;
        this.isVideo = isVideo;
        this.mediaItems = new ArrayList<>();
        for (DocumentFile f : mediaList) mediaItems.add(new MediaItem(f));

        scheduler.scheduleAtFixedRate(this::updateCountdowns, 0, 1, TimeUnit.SECONDS);
    }

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
        boolean isVideoFile = file.getName() != null &&
                file.getName().toLowerCase().matches(".*\\.(mp4|mkv|3gp)$");

        Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .centerCrop()
                .into(holder.imageThumb);

        boolean isSaved = isFileAlreadySaved(file);

        if (isSaved) {
            holder.downloadIcon.setVisibility(View.GONE);
            holder.downloadStatus.setVisibility(View.VISIBLE);
        } else {
            holder.downloadIcon.setVisibility(View.VISIBLE);
            holder.downloadStatus.setVisibility(View.GONE);
        }

        holder.videoIcon.setVisibility(isVideoFile ? View.VISIBLE : View.GONE);

        holder.downloadIcon.setOnClickListener(v -> {
            // Show normal interstitial ad first
            if (AdManager.isAdLoaded()) {
                AdManager.showInterstitial((Activity) context, new AdManager.AdCallback() {
                    @Override
                    public void onAdClosed() {
                        saveFileWithUI(file, holder);
                    }
                });
            } else {
                saveFileWithUI(file, holder);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ImageVideoPreviewActivity.class);
            intent.putExtra(ImageVideoPreviewActivity.EXTRA_URI, file.getUri());
            intent.putExtra(ImageVideoPreviewActivity.EXTRA_IS_VIDEO, isVideoFile);
            context.startActivity(intent);
        });

        holder.expiryTime = item.expiryTime;
        if (!visibleHolders.contains(holder)) visibleHolders.add(holder);
    }

    @Override
    public int getItemCount() { return mediaItems.size(); }

    // ---------- FIXED method for Android 10- ----------
    private boolean isFileAlreadySaved(DocumentFile file) {
        String name = file.getName();
        if (name == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ logic (MediaStore query)
            String selection = MediaStore.Images.Media.DISPLAY_NAME + "=?";
            String[] args = new String[]{name};

            try (Cursor cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.MediaColumns._ID}, selection, args, null)) {
                if (cursor != null && cursor.getCount() > 0) return true;
            } catch (Exception ignored) {}

            try (Cursor cursor = context.getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.MediaColumns._ID}, selection, args, null)) {
                if (cursor != null && cursor.getCount() > 0) return true;
            } catch (Exception ignored) {}

            return false;
        } else {
            // Android 10- (check actual file on disk)
            File folder = new File(android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_PICTURES), "Status Saver");
            File localFile = new File(folder, name);
            return localFile.exists();
        }
    }
    // -----------------------------------------------

    private void saveFileWithUI(DocumentFile file, GalleryViewHolder holder) {
        if (isFileAlreadySaved(file)) return; // prevent duplicates
        actuallySaveFile(file);
        holder.downloadIcon.setVisibility(View.GONE);
        holder.downloadStatus.setVisibility(View.VISIBLE);
    }

    private void actuallySaveFile(DocumentFile file) {
        try {
            String fileName = file.getName();
            if (fileName == null) fileName = "status_" + System.currentTimeMillis();

            String mimeType;
            if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".3gp")) {
                mimeType = "video/mp4";
            } else {
                mimeType = "image/jpeg";
                if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && !fileName.endsWith(".png")) {
                    fileName += ".jpg";
                }
            }

            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Status Saver");
                uri = context.getContentResolver().insert(
                        mimeType.startsWith("video") ?
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI :
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                File folder = new File(android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES), "Status Saver");
                if (!folder.exists()) folder.mkdirs();
                File outFile = new File(folder, fileName);
                uri = Uri.fromFile(outFile);
            }

            try (InputStream in = context.getContentResolver().openInputStream(file.getUri());
                 OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            }

            if (uri != null) context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

            Toast.makeText(context, "Saved to Status Saver ✅", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to save file ❌", Toast.LENGTH_SHORT).show();
        }
    }

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

    public void shutdownScheduler() { scheduler.shutdownNow(); }
}
