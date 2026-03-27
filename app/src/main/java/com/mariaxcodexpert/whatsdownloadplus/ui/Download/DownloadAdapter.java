package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;
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
    private final SavedFilesDB savedFilesDB; // ✅ Added

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
        this.deleteCallback = deleteCallback; // Optional, just for callback
        this.emptyCheckCallback = emptyCheckCallback;
        this.statsManager = statsManager;
        this.savedFilesDB = savedFilesDB; // Already here
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
        if (pos == RecyclerView.NO_POSITION || pos >= mediaUris.size()) return;

        Uri uri = mediaUris.get(pos);
        boolean isVideo = isVideoList.get(pos);

        // Glide Loading
        Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .centerCrop()
                .into(holder.imageThumb);

        holder.videoIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        // 🔥 OPEN FULL SCREEN (Universal Fix for Android 9 to 14+)
        holder.imageThumb.setOnClickListener(v -> {
            int clickPos = holder.getBindingAdapterPosition();
            if (clickPos == RecyclerView.NO_POSITION || clickPos >= mediaUris.size()) return;

            Uri mediaUri = mediaUris.get(clickPos);
            boolean videoFlag = isVideoList.get(clickPos);

            Intent intent = new Intent(context, FullScreenMediaActivity.class);

            // Check if it's a direct file (Android 9/Legacy)
            if ("file".equals(mediaUri.getScheme())) {
                java.io.File file = new java.io.File(mediaUri.getPath());
                if (file.exists()) {
                    // Use FileProvider to safely share file with FullScreenMediaActivity
                    Uri contentUri = androidx.core.content.FileProvider.getUriForFile(context,
                            context.getPackageName() + ".fileprovider", file);

                    intent.putExtra(FullScreenMediaActivity.EXTRA_URI, contentUri.toString());
                    intent.setDataAndType(contentUri, videoFlag ? "video/*" : "image/*");
                } else {
                    android.widget.Toast.makeText(context, "File not found!", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // MediaStore Uri (Android 10+)
                intent.putExtra(FullScreenMediaActivity.EXTRA_URI, mediaUri.toString());
                intent.setDataAndType(mediaUri, videoFlag ? "video/*" : "image/*");
            }

            intent.putExtra(FullScreenMediaActivity.EXTRA_IS_VIDEO, videoFlag);

            // IMPORTANT: Grant read permission to the target activity
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
        });

        // 🔥 DELETE MEDIA
        holder.deleteIcon.setOnClickListener(v -> {
            int clickPos = holder.getBindingAdapterPosition();
            if (clickPos == RecyclerView.NO_POSITION || clickPos >= mediaUris.size()) return;

            // Hum position pass kar rahe hain, deleteFile method baqi handle kar lega
            deleteFile(clickPos, isVideoList.get(clickPos));

            // Optional callback trigger
            if (deleteCallback != null && clickPos < mediaUris.size()) {
                deleteCallback.onDelete(mediaUris.get(clickPos));
            }
        });
    }

    private void deleteFile(int pos, boolean isVideo) {
        if (pos < 0 || pos >= mediaUris.size()) return;

        Uri fileUri = mediaUris.get(pos);
        boolean deleted = false;

        try {
            // 🔥 CASE 1: Android 9 aur Legacy Files (file:// scheme)
            if ("file".equals(fileUri.getScheme())) {
                java.io.File file = new java.io.File(fileUri.getPath());
                if (file.exists()) {
                    deleted = file.delete();
                }
            }
            // 🔥 CASE 2: Android 10+ MediaStore (content:// scheme)
            else {
                deleted = context.getContentResolver().delete(fileUri, null, null) > 0;
            }

            if (deleted) {
                // Database se remove karne ke liye file name nikalna
                String fileName = "";
                if ("file".equals(fileUri.getScheme())) {
                    fileName = new java.io.File(fileUri.getPath()).getName();
                } else {
                    fileName = getFileNameFromUri(fileUri, isVideo);
                }

                if (savedFilesDB != null && fileName != null) {
                    savedFilesDB.removeFile(fileName);
                }

                // Adapter se remove karein
                removeItem(pos);
                Toast.makeText(context, "Deleted successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Could not delete file", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private String getFileNameFromUri(Uri uri, boolean isVideo) {
        // Agar direct file hai toh direct name return karein
        if ("file".equals(uri.getScheme())) {
            return new java.io.File(uri.getPath()).getName();
        }

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

        Uri removedUri = mediaUris.get(pos);
        boolean isVideo = isVideoList.get(pos);

        // Remove from adapter lists
        mediaUris.remove(pos);
        isVideoList.remove(pos);
        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, mediaUris.size());

        // Update DB stats
        if (savedFilesDB != null) {
            String name = getFileNameFromUri(removedUri, isVideo);
            if (name != null) savedFilesDB.removeFile(name);
        }

        // Notify fragment to refresh empty state / counts
        if (emptyCheckCallback != null) {
            emptyCheckCallback.onCheckEmpty();
        }
    }


    @Override
    public int getItemCount() {
        return mediaUris.size();
    }

    public static class StatusViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb, videoIcon, deleteIcon;

        public StatusViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            videoIcon = itemView.findViewById(R.id.videoIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }
    }
}
