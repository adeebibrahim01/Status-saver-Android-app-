package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.app.Activity;
import android.content.ContentResolver;
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
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.model.NotificationScheduler;
import com.mariaxcodexpert.whatsdownloadplus.model.StatusStorage;

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
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private RecyclerView recyclerView;

    public ImagesAndVideoAdapter(Context context, RequestManager glide, boolean isVideo) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.glide = glide;
        this.isVideo = isVideo;
        startCountdownUpdater();
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

        boolean isVideoFile = isVideoFile(file);
        holder.statusId = Math.abs(file.getUri().toString().hashCode());

        // Glide Loading
        glide.load(file.getUri())
                .override(250, 250)
                .centerCrop()
                .placeholder(R.drawable.image_bg)
                .into(holder.imageThumb);

        holder.videoIcon.setVisibility(isVideoFile ? View.VISIBLE : View.GONE);
        holder.imageThumb.setOnClickListener(v -> openPreview(file, isVideoFile));

        // 🔥 REAL-TIME CHECK (Using MediaStore Query)
        boolean saved = isFileAlreadySaved(file);
        updateUIState(holder, false, saved);

        holder.downloadIcon.setOnClickListener(v -> {
            if (!isFileAlreadySaved(file)) {
                saveFileWithAd(file, holder);
            } else {
                Toast.makeText(context, "Already Saved in Gallery", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveFileWithAd(DocumentFile file, GalleryViewHolder holder) {
        if (context instanceof Activity && AdManager.canRequestAds() && AdManager.isAdLoaded()) {
            AdManager.showInterstitial((Activity) context, () -> saveFile(file, holder));
        } else {
            saveFile(file, holder);
            if (context instanceof Activity) AdManager.preloadAd(context.getApplicationContext());
        }
    }

    private void saveFile(DocumentFile file, GalleryViewHolder holder) {
        updateUIState(holder, true, false);

        executor.execute(() -> {
            try {
                String name = file.getName();
                String mime = isVideoFile(file) ? "video/mp4" : "image/jpeg";
                String subFolder = Environment.DIRECTORY_PICTURES + "/Status Saver";

                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, subFolder);

                Uri collection = isVideoFile(file)
                        ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

                Uri destUri = context.getContentResolver().insert(collection, values);

                if (destUri != null) {
                    try (InputStream in = context.getContentResolver().openInputStream(file.getUri());
                         OutputStream out = context.getContentResolver().openOutputStream(destUri)) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                    }

                    handler.post(() -> {
                        int position = holder.getBindingAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            // 🔥 Refresh specific item to trigger re-check
                            notifyItemChanged(position);
                        }
                        Toast.makeText(context, "Saved Successfully!", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                handler.post(() -> updateUIState(holder, false, false));
            }
        });
    }

// ... (Sirf wo method change kiya hai jo crash kar raha tha)

    private void updateUIState(GalleryViewHolder holder, boolean isProcessing, boolean isSaved) {
        if (holder == null || holder.downloadIcon == null) return;

        // Yahan holder.downloadProgress (Jo ProgressBar item layout mein hai) ko use karenge
        // Lekin main screen wala progressBar yahan se access nahi karenge.

        if (isProcessing) {
            if (holder.downloadProgress != null) holder.downloadProgress.setVisibility(View.VISIBLE);
            holder.downloadIcon.setVisibility(View.GONE);
        } else {
            if (holder.downloadProgress != null) holder.downloadProgress.setVisibility(View.GONE);
            holder.downloadIcon.setVisibility(View.VISIBLE);

            if (isSaved) {
                holder.downloadIcon.setImageResource(R.drawable.ic_check);
                holder.downloadIcon.setEnabled(false);
                holder.downloadIcon.setAlpha(0.5f);
                if (holder.downloadStatus != null) holder.downloadStatus.setVisibility(View.VISIBLE);
            } else {
                holder.downloadIcon.setImageResource(R.drawable.ic_download);
                holder.downloadIcon.setEnabled(true);
                holder.downloadIcon.setAlpha(1.0f);
                if (holder.downloadStatus != null) holder.downloadStatus.setVisibility(View.GONE);
            }
        }
    }
    // 🔥 IMPROVED CHECK LOGIC (Checks MediaStore for the filename in our folder)
    private boolean isFileAlreadySaved(DocumentFile f) {
        if (f.getName() == null) return false;

        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " +
                MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?";

        String[] selectionArgs = new String[]{
                f.getName(),
                "%" + Environment.DIRECTORY_PICTURES + "/Status Saver%"
        };

        Uri collection = isVideoFile(f)
                ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(collection, new String[]{MediaStore.MediaColumns._ID}, selection, selectionArgs, null)) {
            return cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isVideoFile(DocumentFile f) {
        String n = f.getName();
        return n != null && n.toLowerCase().matches(".*\\.(mp4|mkv|3gp|avi)$");
    }

    // countdown logic logic...
    private void startCountdownUpdater() {
        scheduler.scheduleWithFixedDelay(() -> handler.post(() -> {
            if (recyclerView == null) return;
            long now = System.currentTimeMillis();
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(child);
                if (vh instanceof GalleryViewHolder) {
                    GalleryViewHolder h = (GalleryViewHolder) vh;
                    long rem = h.expiryTime - now;
                    if (rem < 0) rem = 0;
                    long hrs = rem / 3600000;
                    long mins = (rem / 60000) % 60;
                    long secs = (rem / 1000) % 60;
                    h.countdownTimer.setText(rem == 0 ? "Expired" : String.format("%02d:%02d:%02d", hrs, mins, secs));
                }
            }
        }), 0, 1, TimeUnit.SECONDS);
    }

    public void attachRecyclerView(RecyclerView rv) { this.recyclerView = rv; }
    public void shutdownScheduler() { scheduler.shutdownNow(); executor.shutdownNow(); }

    @Override
    public void onViewRecycled(@NonNull GalleryViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.downloadProgress != null) holder.downloadProgress.setVisibility(View.GONE);
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        public int statusId;
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