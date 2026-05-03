package com.mariaxcodexpert.whatsdownloadplus.ui.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;

import java.util.Objects;

public class GalleryDiffCallback extends DiffUtil.ItemCallback<Object> {

    @Override
    public boolean areItemsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
        if (oldItem.getClass() != newItem.getClass()) return false;

        if (oldItem instanceof ImageEntity) {
            return Objects.equals(((ImageEntity) oldItem).fileName, ((ImageEntity) newItem).fileName);
        } else if (oldItem instanceof VideoEntity) {
            return Objects.equals(((VideoEntity) oldItem).fileName, ((VideoEntity) newItem).fileName);
        }

        return false;
    }

    @Override
    public boolean areContentsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
        // Agar items images hain
        if (oldItem instanceof ImageEntity && newItem instanceof ImageEntity) {
            ImageEntity oldImg = (ImageEntity) oldItem;
            ImageEntity newImg = (ImageEntity) newItem;

            // 🔥 Anti-Blink: Sirf zaroori fields check karein
            return oldImg.isDownloaded == newImg.isDownloaded &&
                    oldImg.expiryTime == newImg.expiryTime &&
                    Objects.equals(oldImg.getUri(), newImg.getUri()) &&
                    Objects.equals(oldImg.gallery_path, newImg.gallery_path);
        }

        // Agar items videos hain
        if (oldItem instanceof VideoEntity && newItem instanceof VideoEntity) {
            VideoEntity oldVid = (VideoEntity) oldItem;
            VideoEntity newVid = (VideoEntity) newItem;

            // 🔥 Anti-Blink: Sirf zaroori fields check karein
            return oldVid.isDownloaded == newVid.isDownloaded &&
                    oldVid.expiryTime == newVid.expiryTime &&
                    Objects.equals(oldVid.getUri(), newVid.getUri()) &&
                    Objects.equals(oldVid.gallery_path, newVid.gallery_path);
        }

        return Objects.equals(oldItem, newItem);
    }

    @Nullable
    @Override
    public Object getChangePayload(@NonNull Object oldItem, @NonNull Object newItem) {
        // Sirf Download Tick (Green icon) update karne ke liye payload use karein
        if (oldItem instanceof ImageEntity && newItem instanceof ImageEntity) {
            if (((ImageEntity) oldItem).isDownloaded != ((ImageEntity) newItem).isDownloaded) {
                return "FORCE_TICK_UPDATE";
            }
        } else if (oldItem instanceof VideoEntity && newItem instanceof VideoEntity) {
            if (((VideoEntity) oldItem).isDownloaded != ((VideoEntity) newItem).isDownloaded) {
                return "FORCE_TICK_UPDATE";
            }
        }

        return super.getChangePayload(oldItem, newItem);
    }
}