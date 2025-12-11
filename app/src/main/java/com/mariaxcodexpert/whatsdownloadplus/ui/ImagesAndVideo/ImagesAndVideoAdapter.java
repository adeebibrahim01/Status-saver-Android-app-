package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

public class ImagesAndVideoAdapter extends RecyclerView.Adapter<ImagesAndVideoAdapter.GalleryViewHolder> {

    private final Context context;
    private List<MediaItem> mediaItems = new ArrayList<>();
    private final List<GalleryViewHolder> visibleHolders = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isVideo;
    private final Set<String> savedFilesCache = new HashSet<>();

    private static final int EXPIRY_HOURS = 24;
    private static final String IMAGE_EXT = ".jpg";
    private static final String VIDEO_EXT = ".mp4";

    public static class MediaItem {
        final DocumentFile file;
        final long expiryTime;

        public MediaItem(DocumentFile file) {
            this.file = file;
            this.expiryTime = file.lastModified() + TimeUnit.MILLISECONDS.convert(EXPIRY_HOURS, TimeUnit.HOURS);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MediaItem)) return false;
            return file.getUri().equals(((MediaItem) obj).file.getUri());
        }
    }

    public ImagesAndVideoAdapter(Context context, List<DocumentFile> mediaList, boolean isVideo) {
        this.context = context;
        this.isVideo = isVideo;
        updateData(mediaList, isVideo);
        startCountdownUpdater();
    }

    public void updateData(List<DocumentFile> newFiles, boolean isVideo) {
        this.isVideo = isVideo;
        List<MediaItem> newItems = new ArrayList<>();
        for (DocumentFile f : newFiles) newItems.add(new MediaItem(f));

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return mediaItems.size(); }
            @Override
            public int getNewListSize() { return newItems.size(); }
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mediaItems.get(oldItemPosition).file.getUri()
                        .equals(newItems.get(newItemPosition).file.getUri());
            }
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return mediaItems.get(oldItemPosition).expiryTime
                        == newItems.get(newItemPosition).expiryTime;
            }
        });

        mediaItems.clear();
        mediaItems.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
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

        Glide.with(context)
                .load(file.getUri())
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .centerCrop()
                .into(holder.imageThumb);

        boolean videoFile = isVideoFile(file);
        holder.videoIcon.setVisibility(videoFile ? View.VISIBLE : View.GONE);
        setDownloadState(holder, isFileAlreadySaved(file));

        holder.downloadIcon.setOnClickListener(v -> showAdThenSave(file, holder));
        holder.itemView.setOnClickListener(v -> openPreview(file, videoFile));

        holder.expiryTime = item.expiryTime;
        if (!visibleHolders.contains(holder)) visibleHolders.add(holder);
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    private boolean isVideoFile(DocumentFile file) {
        String name = file.getName();
        return name != null && name.toLowerCase().matches(".*\\.(mp4|mkv|3gp)$");
    }

    private void setDownloadState(GalleryViewHolder holder, boolean isSaved) {
        holder.downloadIcon.setVisibility(isSaved ? View.GONE : View.VISIBLE);
        holder.downloadStatus.setVisibility(isSaved ? View.VISIBLE : View.GONE);
    }

    private boolean isFileAlreadySaved(DocumentFile file) {
        String name = file.getName();
        if (name == null) return false;
        if (savedFilesCache.contains(name)) return true;

        boolean saved = isFileInMediaStore(name) || isFileOnDisk(name);
        if (saved) savedFilesCache.add(name);
        return saved;
    }

    private boolean isFileInMediaStore(String name) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;
        return queryMediaStore(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, name)
                || queryMediaStore(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, name);
    }

    private boolean queryMediaStore(Uri collection, String name) {
        String selection = MediaStore.Images.Media.DISPLAY_NAME + "=?";
        String[] args = new String[]{name};
        try (Cursor cursor = context.getContentResolver().query(collection, new String[]{MediaStore.MediaColumns._ID}, selection, args, null)) {
            return cursor != null && cursor.getCount() > 0;
        } catch (Exception ignored) { return false; }
    }

    private boolean isFileOnDisk(String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return false;
        File folder = new File(android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_PICTURES), "Status Saver");
        return new File(folder, name).exists();
    }

    private void showAdThenSave(DocumentFile file, GalleryViewHolder holder) {
        if (!(context instanceof Activity)) {
            saveFileWithUI(file, holder);
            return;
        }
        Activity activity = (Activity) context;

        if (AdManager.canRequestAds() && AdManager.isAdLoaded()) {
            AdManager.showInterstitial(activity, () -> saveFileWithUI(file, holder));
        } else {
            saveFileWithUI(file, holder);
            AdManager.init(context); // preload ad
        }
    }



    private void openPreview(DocumentFile file, boolean isVideoFile) {
        Intent intent = new Intent(context, ImageVideoPreviewActivity.class);
        intent.putExtra(ImageVideoPreviewActivity.EXTRA_URI, file.getUri());
        intent.putExtra(ImageVideoPreviewActivity.EXTRA_IS_VIDEO, isVideoFile);
        context.startActivity(intent);
    }

    private void saveFileWithUI(DocumentFile file, GalleryViewHolder holder) {
        if (isFileAlreadySaved(file)) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            saveFile(file);
            handler.post(() -> setDownloadState(holder, true));
        });
    }

    private void saveFile(DocumentFile file) {
        try {
            String fileName = getFileName(file);
            String mimeType = getMimeType(fileName);
            Uri uri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Status Saver");

                uri = context.getContentResolver().insert(
                        mimeType.startsWith("video") ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                File folder = new File(android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES), "Status Saver");
                if (!folder.exists()) folder.mkdirs();
                uri = Uri.fromFile(new File(folder, fileName));
            }

            try (InputStream in = context.getContentResolver().openInputStream(file.getUri());
                 OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            }

            if (uri != null) context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

        } catch (Exception e) {
            e.printStackTrace();
            handler.post(() -> Toast.makeText(context, "Failed to save file ❌", Toast.LENGTH_SHORT).show());
        }
    }

    private String getFileName(DocumentFile file) {
        String name = file.getName();
        if (name == null) name = "status_" + System.currentTimeMillis();
        if (!name.matches(".*\\.(jpg|jpeg|png|mp4|mkv|3gp)$")) {
            name += isVideoFile(file) ? VIDEO_EXT : IMAGE_EXT;
        }
        return name;
    }

    private String getMimeType(String fileName) {
        return fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".3gp")
                ? "video/mp4" : "image/jpeg";
    }

    private void startCountdownUpdater() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (GalleryViewHolder holder : visibleHolders) {
                    long remaining = holder.expiryTime - now;
                    holder.countdownTimer.post(() -> updateCountdown(holder, remaining));
                }
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void updateCountdown(GalleryViewHolder holder, long remaining) {
        if (remaining <= 0) {
            holder.countdownTimer.setText("Expired");
            holder.countdownTimer.setTextColor(0xFFFF0000);
        } else {
            long hours = TimeUnit.MILLISECONDS.toHours(remaining);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;
            holder.countdownTimer.setText(String.format("Expires in %02d:%02d:%02d", hours, minutes, seconds));
            holder.countdownTimer.setTextColor(hours < 1 ? 0xFFFF0000 : (hours < 6 ? 0xFFFFA500 : 0xFF00FF00));
        }
    }

    public static class GalleryViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageThumb, downloadIcon, videoIcon, downloadStatus;
        final TextView countdownTimer;
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

    public void shutdownScheduler() {
        handler.removeCallbacksAndMessages(null);
        visibleHolders.clear();
    }
}
