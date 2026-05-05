package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class OnlineMediaViewModel extends ViewModel {

    // Main list jo UI observe karegi
    private final MutableLiveData<List<MediaItem>> mediaList = new MutableLiveData<>(new ArrayList<>());

    // Pagination aur state maintain karne ke liye variables
    private String lastQuery = "";
    private int lastPage = 1;

    public LiveData<List<MediaItem>> getMediaList() {
        return mediaList;
    }

    public void setMediaList(List<MediaItem> list) {
        // 🔥 Thread Safety: UI thread check logic
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            mediaList.setValue(list);
        } else {
            mediaList.postValue(list);
        }
    }

    /**
     * Fragment jab scroll karke naya data laye ga,
     * tab ye method purani list mein naya data append (add) kar dega.
     */
    public void addMediaItems(List<MediaItem> newItems) {
        List<MediaItem> currentList = mediaList.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }

        List<MediaItem> updatedList = new ArrayList<>(currentList);
        updatedList.addAll(newItems);

        setMediaList(updatedList);
    }

    /**
     * Kisi specific media ka download status update karne ke liye
     */
    public void updateDownloadStatus(String url, boolean status) {
        List<MediaItem> currentList = mediaList.getValue();
        if (currentList != null) {
            boolean isFound = false;

            // Media search logic
            for (MediaItem item : currentList) {
                String matchUrl = item.isVideo() ? item.getVideoUrl() : item.getUrl();
                if (matchUrl != null && matchUrl.equals(url)) {
                    if (item.isDownloaded() != status) {
                        item.setDownloaded(status);
                        isFound = true;
                    }
                }
            }

            // Agar status change hua hai to UI notify karein
            if (isFound) {
                setMediaList(new ArrayList<>(currentList));
            }
        }
    }

    // --- State Management for Fragment Configuration Changes ---

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
        this.lastPage = page;
    }

    /**
     * Search change hone par list ko saaf karne ke liye
     */
    public void clearList() {
        lastPage = 1;
        setMediaList(new ArrayList<>());
    }
}