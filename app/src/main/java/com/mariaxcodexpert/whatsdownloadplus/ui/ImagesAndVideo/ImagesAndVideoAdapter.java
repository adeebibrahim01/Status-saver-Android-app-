package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ImagesAndVideoAdapter extends ListAdapter<DocumentFile, ImagesAndVideoAdapter.GalleryViewHolder> {

    private final Context context;
    private final RequestManager glide;
    private final boolean isVideo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ScheduledExecutorService scheduler;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private RecyclerView recyclerView;

    public ImagesAndVideoAdapter(Context context, RequestManager glide, boolean isVideo) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.glide = glide;
        this.isVideo = isVideo;
    }

    private static final DiffUtil.ItemCallback<DocumentFile> DIFF_CALLBACK = new DiffUtil.ItemCallback<DocumentFile>() {
        @Override
        public boolean areItemsTheSame(@NonNull DocumentFile oldItem, @NonNull DocumentFile newItem) {
            return oldItem.getUri().equals(newItem.getUri());
        }
        @Override
        public boolean areContentsTheSame(@NonNull DocumentFile oldItem, @NonNull DocumentFile newItem) {
            return oldItem.lastModified() == newItem.lastModified();
        }
    };

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_images_videos, parent, false);
        return new GalleryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        DocumentFile file = getItem(position);
        if (file == null) return;

        // 24 Hours expiry logic
        holder.expiryTime = file.lastModified() + 86400000L;

        // --- SILENT NOTIFICATION SCHEDULING ---
        String fileName = file.getName();
        if (fileName != null) {
            int statusId = fileName.hashCode();

            // Sirf unhi statuses ko schedule karein jo pehle nahi hue
            if (!com.mariaxcodexpert.whatsdownloadplus.model.StatusStorage.isNotified(context, statusId, 1)) {
                com.mariaxcodexpert.whatsdownloadplus.model.NotificationScheduler.schedule(
                        context,
                        statusId,
                        holder.expiryTime,
                        isVideoFile(file)
                );
            }
        }

        // --- UI STATE & RESET (Optimization for smooth scrolling) ---
        if (holder.downloadProgress != null) {
            holder.downloadProgress.setVisibility(View.GONE);
            holder.downloadProgress.setIndeterminate(true);
        }

        if (holder.downloadIcon != null) {
            // Position binding adapter se lena best practice hai
            int currentPos = holder.getBindingAdapterPosition();
            holder.downloadIcon.setVisibility(View.VISIBLE);
            holder.downloadIcon.setEnabled(true);
            holder.downloadIcon.setOnClickListener(v -> saveFileWithAd(file, holder, currentPos));
        }

        if (holder.downloadStatus != null) {
            holder.downloadStatus.setVisibility(View.GONE);
        }

        if (holder.videoIcon != null) {
            holder.videoIcon.setVisibility(isVideoFile(file) ? View.VISIBLE : View.GONE);
        }

        // --- GLIDE IMAGE LOADING ---
        glide.load(file.getUri())
                .override(400, 400) // Quality aur speed ka balance
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageThumb);

        // Preview click listener
        holder.imageThumb.setOnClickListener(v -> openPreview(file, isVideoFile(file)));

        // Saved state check (Background thread par fast check)
        checkSavedState(file, holder, position);
    }
    private void checkSavedState(DocumentFile file, GalleryViewHolder holder, int position) {
        executor.execute(() -> {
            boolean isSaved = isFileInFolder(file.getName());
            handler.post(() -> {
                if (holder.getBindingAdapterPosition() == position) {
                    updateUIState(holder, false, isSaved);
                }
            });
        });
    }

    private void saveFileWithAd(DocumentFile file, GalleryViewHolder holder, int position) {
        if (position == RecyclerView.NO_POSITION) return;

        if (context instanceof Activity && AdManager.canRequestAds() && AdManager.isAdLoaded()) {
            AdManager.showInterstitial((Activity) context, () -> saveFile(file, holder, position));
        } else {
            saveFile(file, holder, position);
            if (context instanceof Activity) AdManager.preloadAd(context.getApplicationContext());
        }
    }

    private void saveFile(DocumentFile file, GalleryViewHolder holder, int position) {
        updateUIState(holder, true, false);

        executor.execute(() -> {
            boolean success = false;
            try {
                String name = file.getName();
                String mime = isVideoFile(file) ? "video/mp4" : "image/jpeg";
                String relativePath = Environment.DIRECTORY_PICTURES + "/Status Saver";

                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);

                Uri collection = isVideoFile(file) ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                Uri destUri = context.getContentResolver().insert(collection, values);

                if (destUri != null) {
                    try (InputStream in = context.getContentResolver().openInputStream(file.getUri());
                         OutputStream out = context.getContentResolver().openOutputStream(destUri)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                    }
                    success = true;
                }
            } catch (Exception e) {
                success = false;
            }

            final boolean finalSuccess = success;
            handler.post(() -> {
                if (holder.getBindingAdapterPosition() == position) {
                    updateUIState(holder, false, finalSuccess);
                    if (finalSuccess) {
                        Toast.makeText(context, "Saved Successfully! ✅", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private void updateUIState(GalleryViewHolder holder, boolean isProcessing, boolean isSaved) {
        if (holder.downloadProgress != null)
            holder.downloadProgress.setVisibility(isProcessing ? View.VISIBLE : View.GONE);

        if (holder.downloadIcon != null) {
            holder.downloadIcon.setVisibility((isProcessing || isSaved) ? View.GONE : View.VISIBLE);
        }

        if (holder.downloadStatus != null) {
            holder.downloadStatus.setVisibility((!isProcessing && isSaved) ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isFileInFolder(String fileName) {
        if (fileName == null) return false;
        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " + MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = new String[]{fileName, "%Status Saver%"};

        // Check both collections (Images & Videos)
        Uri[] collections = {MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI};

        for (Uri collection : collections) {
            try (Cursor cursor = context.getContentResolver().query(collection, new String[]{MediaStore.MediaColumns._ID}, selection, selectionArgs, null)) {
                if (cursor != null && cursor.getCount() > 0) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private boolean isVideoFile(DocumentFile f) {
        String n = f.getName();
        return n != null && (n.toLowerCase().endsWith(".mp4") || n.toLowerCase().endsWith(".mkv") || n.toLowerCase().endsWith(".3gp"));
    }

    public void startCountdownUpdater(RecyclerView rv) {
        this.recyclerView = rv;
        if (scheduler != null) scheduler.shutdownNow();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> handler.post(() -> {
            if (recyclerView == null) return;
            long now = System.currentTimeMillis();
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                if (vh instanceof GalleryViewHolder) {
                    GalleryViewHolder h = (GalleryViewHolder) vh;
                    long rem = h.expiryTime - now;
                    if (h.countdownTimer != null) {
                        if (rem <= 0) h.countdownTimer.setText("Expired");
                        else {
                            long hrs = rem / 3600000;
                            long mins = (rem / 60000) % 60;
                            long secs = (rem / 1000) % 60;
                            h.countdownTimer.setText(String.format("Expires in %02d:%02d:%02d", hrs, mins, secs));
                         }
                    }
                }
            }
        }), 0, 1, TimeUnit.SECONDS);
    }

    public void shutdown() {
        if (scheduler != null) scheduler.shutdownNow();
        executor.shutdownNow();
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb, downloadIcon, videoIcon, downloadStatus;
        TextView countdownTimer;
        ProgressBar downloadProgress;
        long expiryTime;

        GalleryViewHolder(@NonNull View v) {
            super(v);
            imageThumb = v.findViewById(R.id.imageThumb);
            downloadIcon = v.findViewById(R.id.downloadIcon);
            downloadStatus = v.findViewById(R.id.downloadStatus);
            videoIcon = v.findViewById(R.id.videoIcon);
            countdownTimer = v.findViewById(R.id.countdownTimer);
            downloadProgress = v.findViewById(R.id.downloadProgress);
        }
    }

    private void openPreview(DocumentFile file, boolean isVideo) {
        Intent intent = new Intent(context, ImageVideoPreviewActivity.class);
        intent.putExtra("uri", file.getUri().toString());
        intent.putExtra("is_video", isVideo);
        context.startActivity(intent);
    }
}