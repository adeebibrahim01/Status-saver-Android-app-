package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.Permissions.PermissionsActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.File;
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.StatusViewHolder> {

    private final Context context;
    private final List<Uri> mediaUris;
    private final List<Boolean> isVideoList;
    private final DeleteCallback deleteCallback;

    // Interface to handle delete from fragment
    public interface DeleteCallback {
        void onDelete(int position);
    }

    public DownloadAdapter(Context context, List<Uri> mediaUris, List<Boolean> isVideoList,
                           PermissionsActivity permissionsActivity, DeleteCallback deleteCallback) {
        this.context = context;
        this.mediaUris = mediaUris;
        this.isVideoList = isVideoList;
        this.deleteCallback = deleteCallback;
    }

    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_downloaded_files, parent, false);
        return new StatusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        Uri uri = mediaUris.get(position);
        boolean isVideo = isVideoList.get(position);

        // Load thumbnail
        Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .centerCrop()
                .into(holder.imageThumb);

        holder.videoIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        holder.deleteIcon.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            // Call fragment's delete callback if provided
            if (deleteCallback != null) {
                deleteCallback.onDelete(pos);
            } else {
                // Fallback: delete directly from adapter
                deleteFile(pos, isVideo);
            }
        });
    }

    // Fallback delete if no fragment callback
    private void deleteFile(int pos, boolean isVideo) {
        Uri fileUri = mediaUris.get(pos);
        boolean deleted = false;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                Uri contentUri = isVideo ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                long id = ContentUris.parseId(fileUri);
                Uri deleteUri = ContentUris.withAppendedId(contentUri, id);
                deleted = resolver.delete(deleteUri, null, null) > 0;
            } else {
                File file = new File(fileUri.getPath());
                deleted = file.exists() && file.delete();
            }

            if (deleted) {
                removeItem(pos);
                Toast.makeText(context, "Deleted successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Can't delete file! Check folder permissions.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error deleting file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void removeItem(int pos) {
        mediaUris.remove(pos);
        isVideoList.remove(pos);
        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, mediaUris.size());
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
