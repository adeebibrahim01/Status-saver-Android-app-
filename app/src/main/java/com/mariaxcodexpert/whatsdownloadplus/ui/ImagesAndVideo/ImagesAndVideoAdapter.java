package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.util.ArrayList;
import java.util.List;

public class ImagesAndVideoAdapter extends RecyclerView.Adapter<ImagesAndVideoAdapter.GalleryViewHolder> {

    private final Context context;
    private List<DocumentFile> mediaList;
    private boolean isVideo;
    private final OnDownloadClickListener downloadListener;
    private final OnItemClickListener itemClickListener;

    // Callback for download
    public interface OnDownloadClickListener {
        void onDownload(DocumentFile file);
    }

    // Callback for item click to switch tabs
    public interface OnItemClickListener {
        void onItemClick(DocumentFile file);
    }

    public ImagesAndVideoAdapter(Context context,
                                 List<DocumentFile> mediaList,
                                 boolean isVideo,
                                 OnDownloadClickListener downloadListener,
                                 OnItemClickListener itemClickListener) {
        this.context = context;
        this.mediaList = new ArrayList<>(mediaList);
        this.isVideo = isVideo;
        this.downloadListener = downloadListener;
        this.itemClickListener = itemClickListener; // store it
    }


    /** Update the adapter data dynamically */
    public void updateData(List<DocumentFile> newMediaList, boolean isVideo) {
        this.mediaList = new ArrayList<>(newMediaList);
        this.isVideo = isVideo;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_images_videos, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        DocumentFile file = mediaList.get(position);
        Uri uri = file.getUri();

        Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .centerCrop()
                .into(holder.imageThumb);

        // Check if file exists in Status Saver
        boolean isSaved = isFileSavedInStatusSaver(file.getName(), file.getType());
        holder.downloadIcon.setVisibility(isSaved ? View.GONE : View.VISIBLE);
        holder.downloadStatus.setVisibility(isSaved ? View.VISIBLE : View.GONE);

        // Show video overlay icon
        holder.videoIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        // Download click
        holder.downloadIcon.setOnClickListener(v -> {
            if (downloadListener != null) {
                downloadListener.onDownload(file);
                holder.downloadIcon.setVisibility(View.GONE);
                holder.downloadStatus.setVisibility(View.VISIBLE);
            }
        });

        // Item click → tell fragment which tab to open
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) itemClickListener.onItemClick(file);
        });

    }
//okay working
    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    /** Check if file already saved in Status Saver */
    private boolean isFileSavedInStatusSaver(String fileName, String mimeType) {
        if (fileName == null) return false;

        Uri collection;
        String selection;
        String[] selectionArgs;

        if (mimeType != null && mimeType.startsWith("video")) {
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            selection = MediaStore.Video.Media.DISPLAY_NAME + "=? AND " +
                    MediaStore.Video.Media.RELATIVE_PATH + " LIKE ?";
            selectionArgs = new String[]{fileName, "%Status Saver%"};
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            selection = MediaStore.Images.Media.DISPLAY_NAME + "=? AND " +
                    MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
            selectionArgs = new String[]{fileName, "%Status Saver%"};
        }

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                new String[]{MediaStore.MediaColumns._ID},
                selection,
                selectionArgs,
                null
        )) {
            return cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb, downloadIcon, videoIcon;
        TextView downloadStatus;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            downloadIcon = itemView.findViewById(R.id.downloadIcon);
            downloadStatus = itemView.findViewById(R.id.downloadStatus);
            videoIcon = itemView.findViewById(R.id.videoIcon);
        }
    }
}
