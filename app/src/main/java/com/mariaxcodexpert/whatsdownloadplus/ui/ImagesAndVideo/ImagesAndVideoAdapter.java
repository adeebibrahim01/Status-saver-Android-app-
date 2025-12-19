package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.PushNotificationHelper;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.Home.DownloadStatsManager;

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

    private final SavedFilesDB savedFilesDB;

    public ImagesAndVideoAdapter(Context context, RequestManager glide, boolean isVideo, SavedFilesDB savedFilesDB) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.glide = glide;
        this.isVideo = isVideo;
        this.savedFilesDB = savedFilesDB;
        startCountdownUpdater();
    }


    private static final DiffUtil.ItemCallback<DocumentFile> DIFF_CALLBACK = new DiffUtil.ItemCallback<DocumentFile>() {
        @Override
        public boolean areItemsTheSame(@NonNull DocumentFile oldItem, @NonNull DocumentFile newItem) {
            return oldItem.getUri().equals(newItem.getUri());
        }

        @Override
        public boolean areContentsTheSame(@NonNull DocumentFile oldItem, @NonNull DocumentFile newItem) {
            boolean lastModifiedSame = oldItem.lastModified() == newItem.lastModified();
            boolean savedStateSame = oldItem.getName() != null && newItem.getName() != null &&
                    oldItem.getName().equals(newItem.getName());
            return lastModifiedSame && savedStateSame;
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

        // Only set expiryTime once
        if (holder.expiryTime == 0) {
            long fileTime = getStatusCreationTime(file);  // <--- Use creation time here
            holder.expiryTime = fileTime + TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);
            holder.notificationSent = false;
        }

        // Load thumbnail
        glide.load(file.getUri())
                .override(200, 200)
                .thumbnail(0.1f)
                .placeholder(R.drawable.image_bg)
                .into(holder.imageThumb);

        holder.imageThumb.setOnClickListener(v -> openPreview(file, isVideoFile(file)));
        holder.downloadIcon.setOnClickListener(v -> saveFileWithAd(file, holder));

        setDownloadState(holder, isFileAlreadySaved(file));
    }


    private long getStatusCreationTime(DocumentFile file) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.nio.file.Path path = java.nio.file.Paths.get(file.getUri().getPath());
                java.nio.file.attribute.BasicFileAttributes attr = java.nio.file.Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class);
                return attr.creationTime().toMillis();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // fallback
        long t = file.lastModified();
        return t > 0 ? t : System.currentTimeMillis();
    }




    public void attachRecyclerView(RecyclerView rv) {
        this.recyclerView = rv;
    }

    private void openPreview(DocumentFile file, boolean isVideo) {
        if (file == null || !file.exists()) return;
        android.content.Intent intent = new android.content.Intent(context, ImageVideoPreviewActivity.class);
        intent.putExtra(ImageVideoPreviewActivity.EXTRA_URI, file.getUri());
        intent.putExtra(ImageVideoPreviewActivity.EXTRA_IS_VIDEO, isVideo);
        context.startActivity(intent);
    }

    private void saveFileWithAd(DocumentFile file, GalleryViewHolder holder) {
        if (file == null || isFileAlreadySaved(file) || isFileAlreadyInMediaStore(file.getName())) return;

        // Disable download icon immediately
        if (holder.downloadIcon != null) holder.downloadIcon.setEnabled(false);

        // Start saving
        saveFile(file, holder);

        // Show ad if possible
        if (context instanceof Activity && AdManager.canRequestAds()) {
            AdManager.showInterstitial((Activity) context, null);
        }
    }


    private boolean isFileAlreadyInMediaStore(String fileName) {
        String[] projection = { MediaStore.MediaColumns._ID };
        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=?";
        String[] args = { fileName };

        Uri uri = isVideo
                ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        try (Cursor c = context.getContentResolver()
                .query(uri, projection, selection, args, null)) {
            return c != null && c.moveToFirst();
        }
    }


    private void saveFile(DocumentFile file, GalleryViewHolder holder) {
        executor.execute(() -> {
            Uri uri = null;
            try {
                String name = file.getName();
                String mime = isVideoFile(file) ? "video/mp4" : "image/jpeg";

                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Status Saver");

                uri = context.getContentResolver().insert(
                        mime.startsWith("video")
                                ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        v);

                if (uri == null) throw new Exception("Uri null");

                long total = file.length();
                long copied = 0;

                // 🔄 SHOW PROGRESS BAR (null-safe)
                handler.post(() -> {
                    if (holder.downloadProgress != null) {
                        if (holder.downloadIcon != null) holder.downloadIcon.setVisibility(View.GONE);
                        holder.downloadProgress.setVisibility(View.VISIBLE);
                        holder.downloadProgress.setProgress(0);
                    }
                });

                try (InputStream in = context.getContentResolver().openInputStream(file.getUri());
                     OutputStream out = context.getContentResolver().openOutputStream(uri)) {

                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        out.write(buf, 0, r);
                        copied += r;

                        int p = (int) ((copied * 100) / total);
                        int progress = p;

                        // 🔁 Update progress (null-safe)
                        handler.post(() -> {
                            if (holder.downloadProgress != null) {
                                holder.downloadProgress.setProgress(progress);
                            }
                        });
                    }
                }
// ✅ SUCCESS (AFTER FULL COPY)
                handler.post(() -> {
                    if (holder.downloadProgress != null) holder.downloadProgress.setVisibility(View.GONE);
                    if (holder.downloadIcon != null) holder.downloadIcon.setVisibility(View.GONE);


                    if (name != null) savedFilesDB.addFile(name);

                    // Set UI state
                    setDownloadState(holder, true);

                    // Add download stat with timestamp
                    long timestamp = file.lastModified(); // file ka actual last modified time
                    if (timestamp <= 0) timestamp = System.currentTimeMillis(); // fallback

                });


            } catch (Exception e) {
                rollbackFailedDownload(file, uri, holder);
            }
        });
    }

    private void rollbackFailedDownload(DocumentFile file, Uri uri, GalleryViewHolder holder) {
        if (uri != null) {
            try {
                context.getContentResolver().delete(uri, null, null);
            } catch (Exception ignored) {}
        }

        if (file.getName() != null) savedFilesDB.removeFile(file.getName());


        handler.post(() -> {
            if (holder.downloadProgress != null)
                holder.downloadProgress.setVisibility(View.GONE);

            if (holder.downloadIcon != null)
                holder.downloadIcon.setEnabled(true); // re-enable
            setDownloadState(holder, false);
        });
    }



    private boolean isFileAlreadySaved(DocumentFile f) {
        return f.getName() != null && savedFilesDB.isFileSaved(f.getName());
    }


    private void setDownloadState(GalleryViewHolder h, boolean saved) {
        if (h.downloadIcon != null)
            h.downloadIcon.setVisibility(saved ? View.GONE : View.VISIBLE);

        if (h.downloadStatus != null)
            h.downloadStatus.setVisibility(saved ? View.VISIBLE : View.GONE);

        if (h.downloadProgress != null)
            h.downloadProgress.setVisibility(View.GONE);
    }



    private boolean isVideoFile(DocumentFile f) {
        String n = f.getName();
        return n != null && n.toLowerCase().matches(".*\\.(mp4|mkv|3gp)$");
    }


    private void startCountdownUpdater() {
        scheduler.scheduleWithFixedDelay(() -> handler.post(() -> {
            if (recyclerView == null) return;

            long currentTime = System.currentTimeMillis();

            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(child);
                if (!(vh instanceof GalleryViewHolder)) continue;

                GalleryViewHolder holder = (GalleryViewHolder) vh;

                // ❌ Do NOT reset expiryTime here
                long remaining = holder.expiryTime - currentTime;
                if (remaining < 0) remaining = 0;

                long h = remaining / (1000 * 60 * 60);
                long m = (remaining / (1000 * 60)) % 60;
                long s = (remaining / 1000) % 60;

                if (holder.countdownTimer != null) {
                    // Update countdown text
                    holder.countdownTimer.setText(remaining == 0 ? "Expired"
                            : String.format("Expires in %02d:%02d:%02d", h, m, s));

                    // Update text color
                    if (h >= 10) {
                        holder.countdownTimer.setTextColor(0xFF4CAF50); // Green
                    } else if (h >= 3) {
                        holder.countdownTimer.setTextColor(0xFFFFC107); // Yellow
                    } else if (h >= 1) {
                        holder.countdownTimer.setTextColor(0xFFF44336); // Red
                    } else {
                        holder.countdownTimer.setTextColor(0xFF9E9E9E); // Gray when expired
                    }

                    // Send notification exactly when 1 hour remains
                    if (!holder.notificationSent && h == 1 && m == 0 && s == 0) {
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.putExtra("openFragment", "ImagesAndVideo");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        PushNotificationHelper helper = new PushNotificationHelper(context);
                        helper.sendNotification(
                                "Status About to Expire",
                                "A status is about to expire—view it before it disappears in 1 hour.",
                                intent,
                                1001
                        );

                        holder.notificationSent = true;
                    }
                }
            }
        }), 0, 1, TimeUnit.SECONDS);
    }



    public void shutdownScheduler() {
        scheduler.shutdownNow();
        executor.shutdownNow();
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb, downloadIcon, videoIcon, downloadStatus;
        TextView countdownTimer;
        ProgressBar downloadProgress;
        long expiryTime;
        public boolean notificationSent = false;

        GalleryViewHolder(@NonNull View v) {
            super(v);
            imageThumb = v.findViewById(R.id.imageThumb);
            downloadIcon = v.findViewById(R.id.downloadIcon);
            downloadStatus = v.findViewById(R.id.downloadStatus);
            videoIcon = v.findViewById(R.id.videoIcon);
            countdownTimer = v.findViewById(R.id.countdownTimer);

            // ✅ YAHAN PASTE KARO
            downloadProgress = v.findViewById(R.id.downloadProgress);
        }
    }



}
