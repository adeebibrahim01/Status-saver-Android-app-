package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.SmartNotify;
import com.mariaxcodexpert.whatsdownloadplus.ui.Home.DownloadStatsManager;
import com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo.SavedFilesDB;

import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.StatusViewHolder> {

    private final Context context;
    private final List<Uri> mediaUris;
    private final List<Boolean> isVideoList;
    private final DeleteCallback deleteCallback;
    private final EmptyCheckCallback emptyCheckCallback;
    private final DownloadStatsManager statsManager;
    private final SavedFilesDB savedFilesDB;

    public interface DeleteCallback {
        void onDelete(Uri uri);
    }

    public interface EmptyCheckCallback {
        void onCheckEmpty();
    }

    public DownloadAdapter(Context context, List<Uri> mediaUris, List<Boolean> isVideoList,
                           DeleteCallback deleteCallback, EmptyCheckCallback emptyCheckCallback,
                           DownloadStatsManager statsManager, SavedFilesDB savedFilesDB) {
        this.context = context;
        this.mediaUris = mediaUris;
        this.isVideoList = isVideoList;
        this.deleteCallback = deleteCallback;
        this.emptyCheckCallback = emptyCheckCallback;
        this.statsManager = statsManager;
        this.savedFilesDB = savedFilesDB;
    }

    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_downloaded_files, parent, false);
        return new StatusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        int pos = holder.getBindingAdapterPosition();

        // Safety check for binding
        if (pos == RecyclerView.NO_POSITION || pos >= mediaUris.size()) return;

        Uri currentUri = mediaUris.get(pos);
        boolean isVideo = isVideoList.get(pos);

        // --- 1. OPTIMIZED LOADING ---
        Glide.with(context)
                .load(currentUri)
                .override(300, 300)
                .thumbnail(0.1f)
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.imageThumb);

        holder.videoIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        // --- 2. FULL SCREEN CLICK ---
        holder.imageThumb.setOnClickListener(v -> {
            int clickPos = holder.getBindingAdapterPosition();
            if (clickPos == RecyclerView.NO_POSITION || clickPos >= mediaUris.size()) return;

            Intent intent = new Intent(context, FullScreenMediaActivity.class);
            // Important: Use the list with the fresh index
            intent.putExtra("EXTRA_URI", mediaUris.get(clickPos).toString());
            intent.putExtra("EXTRA_IS_VIDEO", isVideoList.get(clickPos));

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });

        // --- 3. SAFE DELETE ACTION ---
        holder.deleteIcon.setOnClickListener(v -> {
            int clickPos = holder.getBindingAdapterPosition();

            // Final validation before touching the list
            if (clickPos != RecyclerView.NO_POSITION && clickPos < mediaUris.size()) {

                // Capture data in local variables BEFORE deleting
                Uri uriToDelete = mediaUris.get(clickPos);
                boolean wasVideo = isVideoList.get(clickPos);

                // Execute deletion logic
                deleteFile(clickPos, wasVideo, v);

                // Use the captured local variable for the callback
                if (deleteCallback != null) {
                    deleteCallback.onDelete(uriToDelete);
                }
            }
        });
    }

    private void deleteFile(int pos, boolean isVideo, View view) {
        if (pos < 0 || pos >= mediaUris.size()) return;

        Uri fileUri = mediaUris.get(pos);
        boolean deleted;

        try {
            // Android 10+ mein hamesha ContentResolver use hota hai
            deleted = context.getContentResolver().delete(fileUri, null, null) > 0;

            if (deleted) {
                String fileName = getFileNameFromUri(fileUri);

                if (savedFilesDB != null && fileName != null) {
                    savedFilesDB.removeFile(fileName);
                }

                removeItem(pos);
                SmartNotify.success(view, "Deleted successfully!");

            } else {
                SmartNotify.error(view, "Could not delete file");
            }

        } catch (Exception e) {
            e.printStackTrace();
            SmartNotify.error(view, "Error: " + e.getMessage());
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String[] projection = { MediaStore.MediaColumns.DISPLAY_NAME };
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void removeItem(int pos) {
        if (pos < 0 || pos >= mediaUris.size()) return;

        mediaUris.remove(pos);
        isVideoList.remove(pos);
        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, mediaUris.size());

        if (emptyCheckCallback != null) {
            emptyCheckCallback.onCheckEmpty();
        }
    }

    @Override
    public int getItemCount() {
        return mediaUris.size();
    }

    public static class StatusViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb, videoIcon;
        com.google.android.material.button.MaterialButton deleteIcon;

        public StatusViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            videoIcon = itemView.findViewById(R.id.videoIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }
    }
}