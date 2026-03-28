package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
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

import java.io.File;
import java.io.FileOutputStream;
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

        // 1. Expiry Logic
        holder.expiryTime = file.lastModified() + 86400000L;

        // 2. Notification Scheduling (Silent)
        String fileName = file.getName();
        if (fileName != null) {
            int statusId = fileName.hashCode();
            if (!com.mariaxcodexpert.whatsdownloadplus.model.StatusStorage.isNotified(context, statusId, 1)) {
                com.mariaxcodexpert.whatsdownloadplus.model.NotificationScheduler.schedule(
                        context,
                        statusId,
                        holder.expiryTime,
                        isVideoFile(file)
                );
            }
        }

        // 3. --- UI STATE RESET (Crucial for Recycler View) ---
        // Resetting progress bar to prevent NPE and UI glitches
        if (holder.downloadProgress != null) {
            holder.downloadProgress.setVisibility(View.GONE);
            holder.downloadProgress.setIndeterminate(true); // Horizontal bar pe ye crash nahi karega agar properly handled ho
        }

        if (holder.downloadIcon != null) {
            holder.downloadIcon.setVisibility(View.VISIBLE);
            holder.downloadIcon.setEnabled(true);

            // Lambda ke andar current position dynamic leni chahiye
            holder.downloadIcon.setOnClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    saveFileWithAd(getItem(currentPos), holder, currentPos);
                }
            });
        }

        if (holder.downloadStatus != null) {
            holder.downloadStatus.setVisibility(View.GONE);
        }

        if (holder.videoIcon != null) {
            holder.videoIcon.setVisibility(isVideoFile(file) ? View.VISIBLE : View.GONE);
        }

        // 4. --- GLIDE IMAGE LOADING (Blink-Free Update) ---
        if (holder.imageThumb != null) {
            glide.load(file.getUri())
                    // Purani image ko placeholder rakhein taake naya load hote waqt white blink na ho
                    .placeholder(holder.imageThumb.getDrawable())
                    .override(400, 400)
                    .centerCrop()
                    // Animations band karne se refreshing ke waqt flickering khatam ho jati hai
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.imageThumb);

            // Preview listener with safety check
            holder.imageThumb.setOnClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    DocumentFile currentFile = getItem(currentPos);
                    if (currentFile != null) {
                        openPreview(currentFile, isVideoFile(currentFile));
                    }
                }
            });
        }

        // 5. --- SAVED STATE CHECK ---
        // Isme hum current position pass kar rahe hain taake recycled holder pe wrong data na dikhe
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ logic (Already working fine)
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, isVideoFile(file) ? "video/mp4" : "image/jpeg");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Status Saver");

                    Uri collection = isVideoFile(file) ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    Uri destUri = context.getContentResolver().insert(collection, values);

                    if (destUri != null) {
                        try (InputStream in = context.getContentResolver().openInputStream(file.getUri());
                             OutputStream out = context.getContentResolver().openOutputStream(destUri)) {
                            copyStream(in, out);
                        }
                        success = true;
                    }
                } else {
                    // 🔥 FIX FOR ANDROID 9 (Permission Denied Fix)
                    File dir = new File(Environment.getExternalStorageDirectory(), "Pictures/Status Saver");
                    if (!dir.exists()) {
                        boolean created = dir.mkdirs();
                        if (!created && !dir.exists()) throw new Exception("Folder create nahi ho saka!");
                    }

                    File destFile = new File(dir, name);

                    // Purani file agar exists karti hai toh delete karein taake overwrite ho sake
                    if (destFile.exists()) destFile.delete();

                    try (InputStream in = context.getContentResolver().openInputStream(file.getUri());
                         FileOutputStream out = new FileOutputStream(destFile)) {
                        copyStream(in, out);
                    }

                    // Media scanner ko lazmi batana hai
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(destFile));
                    context.sendBroadcast(mediaScanIntent);
                    success = true;
                }
            } catch (Exception e) {
                android.util.Log.e("SAVE_ERROR", "Error: " + e.getMessage());
                success = false;
            }

            final boolean finalSuccess = success;
            handler.post(() -> {
                if (holder.getBindingAdapterPosition() == position) {
                    updateUIState(holder, false, finalSuccess);
                    if (finalSuccess) {
                        Toast.makeText(context, "Saved Successfully! ✅", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Save Failed! Check Storage Permission.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }
    // Stream helper (Add this for clean code)
    private void copyStream(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
    }
    private void updateUIState(GalleryViewHolder holder, boolean isProcessing, boolean isSaved) {
        if (holder == null) return;

        // Progress Bar: Sirf downloading ke waqt dikhayein
        if (holder.downloadProgress != null) {
            holder.downloadProgress.setVisibility(isProcessing ? View.VISIBLE : View.GONE);
        }

        // Blink Fix: Processing ke waqt icons ko GONE karne ki bajaye
        // hum unhe invisible rakh sakte hain ya download icon ko hi rehne de sakte hain
        if (holder.downloadIcon != null && holder.downloadStatus != null) {
            if (isProcessing) {
                // Downloading ke waqt blink se bachne ke liye download icon ko dhundla (Alpha) kar dein
                holder.downloadIcon.setVisibility(View.VISIBLE);
                holder.downloadIcon.setAlpha(0.3f);
                holder.downloadStatus.setVisibility(View.GONE);
            } else if (isSaved) {
                // Save ho gaya: Double tick dikhayein
                holder.downloadIcon.setVisibility(View.GONE);
                holder.downloadStatus.setVisibility(View.VISIBLE);
                holder.downloadStatus.setAlpha(1.0f);
            } else {
                // Default: Download icon
                holder.downloadIcon.setVisibility(View.VISIBLE);
                holder.downloadIcon.setAlpha(1.0f);
                holder.downloadStatus.setVisibility(View.GONE);
            }
        }
    }

    private boolean isFileInFolder(String fileName) {
        if (fileName == null) return false;

        try {
            // Android 9/10/11+ sab ke liye behtar tareeka
            File dir = new File(Environment.getExternalStorageDirectory(), "Pictures/Status Saver");
            File file = new File(dir, fileName);
            if (file.exists() && file.length() > 0) return true;

            // Backup for Android 11+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=?";
                String[] selectionArgs = new String[]{fileName};
                Uri collection = isVideoFile(DocumentFile.fromFile(file)) ?
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI :
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

                try (Cursor cursor = context.getContentResolver().query(collection, new String[]{MediaStore.MediaColumns._ID}, selection, selectionArgs, null)) {
                    return cursor != null && cursor.getCount() > 0;
                }
            }
        } catch (Exception ignored) {}

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