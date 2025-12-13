package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
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

import com.bumptech.glide.RequestManager;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ImagesAndVideoAdapter
        extends RecyclerView.Adapter<ImagesAndVideoAdapter.GalleryViewHolder> {

    private final Context context;
    private final RequestManager glide;

    /** 🔒 main data list (guarded by synchronized) */
    private final List<MediaItem> mediaItems = new ArrayList<>();

    /** ✅ thread-safe list for countdown updates */
    private final CopyOnWriteArrayList<GalleryViewHolder> visibleHolders =
            new CopyOnWriteArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean isVideo;

    private final Set<String> savedFilesCache = new HashSet<>();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private final ExecutorService executor =
            Executors.newFixedThreadPool(3);

    private Runnable onDataLoaded;

    private static final int EXPIRY_HOURS = 24;
    private static final String IMAGE_EXT = ".jpg";
    private static final String VIDEO_EXT = ".mp4";
    // Adapter level field (class ke top par hona chahiye)
    private final Object updateLock = new Object();
    private int updateVersion = 0;

    /**
     * Prefetch thumbnails in small batches (safe & async)
     */
    public void prefetchAll() {
        if (executor == null || executor.isShutdown()) return;

        final List<MediaItem> snapshot;
        synchronized (mediaItems) {
            snapshot = new ArrayList<>(mediaItems);
        }

        executor.execute(() -> {
            final int batchSize = 6;

            for (int i = 0; i < snapshot.size(); i += batchSize) {
                int end = Math.min(snapshot.size(), i + batchSize);

                for (int j = i; j < end; j++) {
                    try {
                        glide.load(snapshot.get(j).file.getUri())
                                .override(200, 200)
                                .thumbnail(0.1f)
                                .preload();
                    } catch (Exception ignored) {}
                }

                try {
                    Thread.sleep(80); // decoder breathing space
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }


    /* ======================= MODEL ======================= */

    public static class MediaItem {
        final DocumentFile file;
        final long expiryTime;

        public MediaItem(DocumentFile file) {
            this.file = file;
            this.expiryTime = file.lastModified()
                    + TimeUnit.MILLISECONDS.convert(EXPIRY_HOURS, TimeUnit.HOURS);
        }
    }

    /* ======================= CONSTRUCTOR ======================= */

    public ImagesAndVideoAdapter(Context context,
                                 List<DocumentFile> mediaList,
                                 boolean isVideo,
                                 RequestManager glide,
                                 Runnable onDataLoaded) {

        this.context = context;
        this.glide = glide;
        this.isVideo = isVideo;
        this.onDataLoaded = onDataLoaded;

        startCountdownUpdater();
        updateDataAsync(mediaList, isVideo);
    }

    /* ======================= DATA UPDATE ======================= */


    public void updateDataAsync(List<DocumentFile> newFiles, boolean isVideo) {

        this.isVideo = isVideo;
        if (executor == null || executor.isShutdown()) return;

        // 🔐 generate unique version for THIS call
        final int myVersion;
        synchronized (updateLock) {
            updateVersion++;
            myVersion = updateVersion;
        }

        // 🔒 SNAPSHOT ON CALLER THREAD
        final List<DocumentFile> safeFiles = new ArrayList<>(newFiles);

        executor.execute(() -> {

            List<MediaItem> newItems = new ArrayList<>();
            for (DocumentFile f : safeFiles) {
                newItems.add(new MediaItem(f));
            }

            List<MediaItem> oldItems;
            synchronized (mediaItems) {
                oldItems = new ArrayList<>(mediaItems);
            }

            DiffUtil.DiffResult diffResult =
                    DiffUtil.calculateDiff(new DiffUtil.Callback() {

                        @Override
                        public int getOldListSize() {
                            return oldItems.size();
                        }

                        @Override
                        public int getNewListSize() {
                            return newItems.size();
                        }

                        @Override
                        public boolean areItemsTheSame(int oldPos, int newPos) {
                            return oldItems.get(oldPos).file.getUri()
                                    .equals(newItems.get(newPos).file.getUri());
                        }

                        @Override
                        public boolean areContentsTheSame(int oldPos, int newPos) {
                            return oldItems.get(oldPos).expiryTime
                                    == newItems.get(newPos).expiryTime;
                        }
                    });

            handler.post(() -> {

                // ❌ agar tab change ho chuka hai → ignore old result
                synchronized (updateLock) {
                    if (myVersion != updateVersion) return;
                }

                synchronized (mediaItems) {
                    mediaItems.clear();
                    mediaItems.addAll(newItems);
                }

                diffResult.dispatchUpdatesTo(this);

                if (onDataLoaded != null) {
                    onDataLoaded.run();
                }
            });
        });
    }


    /* ======================= ADAPTER ======================= */

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_images_videos, parent, false);
        return new GalleryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {

        MediaItem item;
        synchronized (mediaItems) {
            if (position >= mediaItems.size()) return;
            item = mediaItems.get(position);
        }

        DocumentFile file = item.file;
        boolean isVideoFile = isVideoFile(file);

        glide.load(file.getUri())
                .override(200, 200)
                .thumbnail(0.1f)
                .placeholder(R.drawable.image_bg)
                .dontAnimate()
                .into(holder.imageThumb);

        holder.videoIcon.setVisibility(isVideoFile ? View.VISIBLE : View.GONE);

        holder.imageThumb.setOnClickListener(v ->
                openPreview(file, isVideoFile));

        holder.downloadIcon.setOnClickListener(v ->
                showAdThenSave(file, holder));

        setDownloadState(holder, isFileAlreadySaved(file));

        holder.expiryTime = item.expiryTime;

        if (!visibleHolders.contains(holder)) {
            visibleHolders.add(holder);
        }
    }

    @Override
    public int getItemCount() {
        synchronized (mediaItems) {
            return mediaItems.size();
        }
    }

    /* ======================= COUNTDOWN ======================= */

    private void startCountdownUpdater() {
        scheduler.scheduleWithFixedDelay(() -> {

            long now = System.currentTimeMillis();

            handler.post(() -> {

                List<GalleryViewHolder> snapshot =
                        new ArrayList<>(visibleHolders);

                for (GalleryViewHolder holder : snapshot) {

                    long remaining = holder.expiryTime - now;
                    if (remaining < 0) remaining = 0;

                    long h = remaining / (1000 * 60 * 60);
                    long m = (remaining / (1000 * 60)) % 60;
                    long s = (remaining / 1000) % 60;

                    String text = remaining == 0
                            ? "Expired"
                            : String.format("Expires in %02d:%02d:%02d", h, m, s);

                    if (!text.equals(holder.countdownTimer.getText().toString())) {
                        holder.countdownTimer.setText(text);
                    }
                }
            });

        }, 0, 1, TimeUnit.SECONDS);
    }

    /* ======================= SAVE FILE ======================= */

    private void showAdThenSave(DocumentFile file, GalleryViewHolder holder) {
        saveFileWithUI(file, holder);

        if (context instanceof Activity && AdManager.canRequestAds()) {
            AdManager.showInterstitial((Activity) context, null);
        } else {
            AdManager.init(context);
        }
    }

    private void saveFileWithUI(DocumentFile file, GalleryViewHolder holder) {
        if (isFileAlreadySaved(file)) return;
        executor.execute(() -> {
            boolean ok = saveFile(file);
            handler.post(() -> {
                if (ok) setDownloadState(holder, true);
                else Toast.makeText(context,
                        "Failed to save ❌", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private boolean saveFile(DocumentFile file) {
        try {
            String name = getFileName(file);
            String mime = isVideoFile(file) ? "video/mp4" : "image/jpeg";

            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            v.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Status Saver");

            Uri uri = context.getContentResolver().insert(
                    mime.startsWith("video")
                            ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            : MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);

            if (uri == null) return false;

            try (InputStream in =
                         context.getContentResolver().openInputStream(file.getUri());
                 OutputStream out =
                         context.getContentResolver().openOutputStream(uri)) {

                byte[] buf = new byte[4096];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* ======================= HELPERS ======================= */

    private void setDownloadState(GalleryViewHolder h, boolean saved) {
        h.downloadIcon.setVisibility(saved ? View.GONE : View.VISIBLE);
        h.downloadStatus.setVisibility(saved ? View.VISIBLE : View.GONE);
    }

    private boolean isFileAlreadySaved(DocumentFile f) {
        String n = f.getName();
        if (n == null) return false;
        if (savedFilesCache.contains(n)) return true;
        return false;
    }

    private String getFileName(DocumentFile f) {
        String n = f.getName();
        if (n == null) n = "status_" + System.currentTimeMillis();
        if (!n.matches(".*\\.(jpg|jpeg|png|mp4)$")) {
            n += isVideoFile(f) ? VIDEO_EXT : IMAGE_EXT;
        }
        return n;
    }

    private boolean isVideoFile(DocumentFile f) {
        String n = f.getName();
        return n != null && n.matches(".*\\.(mp4|mkv|3gp)$");
    }

    private void openPreview(DocumentFile file, boolean isVideo) {
        Intent i = new Intent(context, ImageVideoPreviewActivity.class);
        i.putExtra(ImageVideoPreviewActivity.EXTRA_URI, file.getUri());
        i.putExtra(ImageVideoPreviewActivity.EXTRA_IS_VIDEO, isVideo);
        context.startActivity(i);
    }

    /* ======================= CLEANUP ======================= */

    @Override
    public void onViewRecycled(@NonNull GalleryViewHolder holder) {
        super.onViewRecycled(holder);
        visibleHolders.remove(holder);
    }

    public void shutdownScheduler() {
        scheduler.shutdownNow();
        executor.shutdownNow();
        visibleHolders.clear();
    }

    /* ======================= VIEW HOLDER ======================= */

    static class GalleryViewHolder extends RecyclerView.ViewHolder {

        ImageView imageThumb, downloadIcon, videoIcon, downloadStatus;
        TextView countdownTimer;
        long expiryTime;

        GalleryViewHolder(@NonNull View v) {
            super(v);
            imageThumb = v.findViewById(R.id.imageThumb);
            downloadIcon = v.findViewById(R.id.downloadIcon);
            downloadStatus = v.findViewById(R.id.downloadStatus);
            videoIcon = v.findViewById(R.id.videoIcon);
            countdownTimer = v.findViewById(R.id.countdownTimer);
        }
    }
}
