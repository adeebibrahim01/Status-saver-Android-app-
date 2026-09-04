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

        try {
            if (oldItem instanceof ImageEntity && newItem instanceof ImageEntity) {
                String oldName = ((ImageEntity) oldItem).fileName;
                String newName = ((ImageEntity) newItem).fileName;
                return oldName != null && oldName.equals(newName);
            }

            if (oldItem instanceof VideoEntity && newItem instanceof VideoEntity) {
                String oldName = ((VideoEntity) oldItem).fileName;
                String newName = ((VideoEntity) newItem).fileName;
                return oldName != null && oldName.equals(newName);
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }

    @Override
    public boolean areContentsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
        try {
            if (oldItem instanceof ImageEntity && newItem instanceof ImageEntity) {
                ImageEntity o = (ImageEntity) oldItem;
                ImageEntity n = (ImageEntity) newItem;

                return o.isDownloaded == n.isDownloaded &&
                        o.expiryTime == n.expiryTime &&
                        Objects.equals(o.getUri(), n.getUri()) &&
                        Objects.equals(o.gallery_path, n.gallery_path);
            }

            if (oldItem instanceof VideoEntity && newItem instanceof VideoEntity) {
                VideoEntity o = (VideoEntity) oldItem;
                VideoEntity n = (VideoEntity) newItem;

                return o.isDownloaded == n.isDownloaded &&
                        o.expiryTime == n.expiryTime &&
                        Objects.equals(o.getUri(), n.getUri()) &&
                        Objects.equals(o.gallery_path, n.gallery_path);
            }
        } catch (Exception e) {
            return false;
        }

        return Objects.equals(oldItem, newItem);
    }
    @Nullable
    @Override
    public Object getChangePayload(@NonNull Object oldItem, @NonNull Object newItem) {
        try {
            if (oldItem instanceof ImageEntity && newItem instanceof ImageEntity) {
                if (((ImageEntity) oldItem).isDownloaded != ((ImageEntity) newItem).isDownloaded) {
                    return "FORCE_TICK_UPDATE";
                }
            }

            if (oldItem instanceof VideoEntity && newItem instanceof VideoEntity) {
                if (((VideoEntity) oldItem).isDownloaded != ((VideoEntity) newItem).isDownloaded) {
                    return "FORCE_TICK_UPDATE";
                }
            }
        } catch (Exception e) {
            return null;
        }

        return super.getChangePayload(oldItem, newItem);
    }
}