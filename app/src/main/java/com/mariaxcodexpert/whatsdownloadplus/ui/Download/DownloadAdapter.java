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

        // Load thumbnail using Glide
        Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .centerCrop()
                .into(holder.imageThumb);

        holder.videoIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        // Open full screen media
        holder.imageThumb.setOnClickListener(v -> {
            int clickPos = holder.getBindingAdapterPosition();
            if (clickPos == RecyclerView.NO_POSITION || clickPos >= mediaUris.size()) return;

            Uri mediaUri = mediaUris.get(clickPos);
            boolean videoFlag = isVideoList.get(clickPos);

            Intent intent = new Intent(context, FullScreenMediaActivity.class);
            intent.putExtra(FullScreenMediaActivity.EXTRA_URI, mediaUri);
            intent.putExtra(FullScreenMediaActivity.EXTRA_IS_VIDEO, videoFlag);
            context.startActivity(intent);
        });

        // Delete media
        holder.deleteIcon.setOnClickListener(v -> {
            int clickPos = holder.getBindingAdapterPosition();
            if (clickPos == RecyclerView.NO_POSITION || clickPos >= mediaUris.size()) return;

            Uri uriToDelete = mediaUris.get(clickPos);
            boolean isVideos = isVideoList.get(clickPos);

            deleteFile(clickPos, isVideos);

            if (deleteCallback != null) {
                deleteCallback.onDelete(uriToDelete);
            }
        });
    }

    private void deleteFile(int pos, boolean isVideo) {
        if (pos < 0 || pos >= mediaUris.size()) return;

        Uri fileUri = mediaUris.get(pos);

        try {
            ContentResolver resolver = context.getContentResolver();
            Uri contentUri = isVideo
                    ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            long id = ContentUris.parseId(fileUri);
            Uri deleteUri = ContentUris.withAppendedId(contentUri, id);

            // Remove from DB first
            if (savedFilesDB != null) {
                String name = getFileNameFromUri(fileUri, isVideo);
                if (name != null) savedFilesDB.removeFile(name);
            }

            // Then remove from device
            boolean deleted = resolver.delete(deleteUri, null, null) > 0;

            if (deleted) {
                // Remove from adapter
                removeItem(pos);
                Toast.makeText(context, "Deleted successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Can't delete file! Check permissions.", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error deleting file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private String getFileNameFromUri(Uri uri, boolean isVideo) {
        String[] projection = { MediaStore.MediaColumns.DISPLAY_NAME };
        Uri contentUri = isVideo
                ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        try (Cursor cursor = context.getContentResolver().query(
                contentUri, projection,
                MediaStore.MediaColumns._ID + "=?",
                new String[]{String.valueOf(ContentUris.parseId(uri))}, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
                if (name != null) return name.trim();
            }
        } catch (Exception ignored) {}
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
