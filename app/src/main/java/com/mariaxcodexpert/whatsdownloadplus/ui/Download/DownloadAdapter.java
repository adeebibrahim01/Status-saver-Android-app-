package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
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

import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.StatusViewHolder> {

    private final Context context;
    private final List<Uri> mediaUris;
    private final List<Boolean> isVideoList;
    private final DeleteCallback deleteCallback;
    private final EmptyCheckCallback emptyCheckCallback;

    public interface DeleteCallback {
        void onDelete(Uri uri); // pass URI instead of position
    }

    public interface EmptyCheckCallback {
        void onCheckEmpty();
    }

    public DownloadAdapter(Context context, List<Uri> mediaUris, List<Boolean> isVideoList,
                           DeleteCallback deleteCallback, EmptyCheckCallback emptyCheckCallback) {
        this.context = context;
        this.mediaUris = mediaUris;
        this.isVideoList = isVideoList;
        this.deleteCallback = deleteCallback;
        this.emptyCheckCallback = emptyCheckCallback;
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
                deleteCallback.onDelete(uriToDelete); // Safe deletion callback
            }
        });
    }

    private void deleteFile(int pos, boolean isVideo) {
        if (pos < 0 || pos >= mediaUris.size()) return;

        Uri fileUri = mediaUris.get(pos);
        boolean deleted = false;

        try {
            ContentResolver resolver = context.getContentResolver();
            Uri contentUri = isVideo ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            long id = ContentUris.parseId(fileUri);
            Uri deleteUri = ContentUris.withAppendedId(contentUri, id);
            deleted = resolver.delete(deleteUri, null, null) > 0;

            if (deleted) {
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
        ImageView imageThumb, videoIcon, deleteIcon;

        public StatusViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            videoIcon = itemView.findViewById(R.id.videoIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }
    }
}
