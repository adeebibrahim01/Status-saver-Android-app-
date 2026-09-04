package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class OnlineMediaViewModel extends ViewModel {

    private final MutableLiveData<List<MediaItem>> mediaList = new MutableLiveData<>(new ArrayList<>());

    private String lastQuery = "";
    private int lastPage = 1;

    public LiveData<List<MediaItem>> getMediaList() {
        return mediaList;
    }

    public void setMediaList(List<MediaItem> list) {
        if (list == null) list = new ArrayList<>();

        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            mediaList.setValue(list);
        } else {
            mediaList.postValue(list);
        }
    }

    public void addMediaItems(List<MediaItem> newItems) {
        if (newItems == null || newItems.isEmpty()) return;

        List<MediaItem> currentList = mediaList.getValue();
        List<MediaItem> updatedList = (currentList == null) ? new ArrayList<>() : new ArrayList<>(currentList);

        for (MediaItem newItem : newItems) {
            if (newItem != null) {
                updatedList.add(newItem);
            }
        }

        setMediaList(updatedList);
    }

    public void updateDownloadStatus(String url, boolean status) {
        if (url == null) return;

        List<MediaItem> currentList = mediaList.getValue();
        if (currentList == null || currentList.isEmpty()) return;

        boolean isFound = false;
        // Create a new list for modification to avoid ConcurrentModificationException
        List<MediaItem> newList = new ArrayList<>(currentList);

        for (MediaItem item : newList) {
            if (item == null) continue;

            String matchUrl = item.isVideo() ? item.getVideoUrl() : item.getUrl();
            if (matchUrl != null && matchUrl.equals(url)) {
                if (item.isDownloaded() != status) {
                    item.setDownloaded(status);
                    isFound = true;
                }
            }
        }

        if (isFound) {
            setMediaList(newList);
        }
    }
    public String getLastQuery() {
        return lastQuery;
    }
    public void setLastQuery(String query) {
        this.lastQuery = query;
    }
    public int getLastPage() {
        return lastPage;
    }
    public void setLastPage(int page) {
        this.lastPage = Math.max(1, page);
    }

    public void clearList() {
        lastPage = 1;
        lastQuery = "";
        setMediaList(new ArrayList<>());
    }
}