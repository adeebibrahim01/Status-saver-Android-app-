package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class OnlineMediaViewModel extends ViewModel {

    private final MutableLiveData<List<MediaItem>> mediaList = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<MediaItem>> getMediaList() {
        return mediaList;
    }

    public void setMediaList(List<MediaItem> list) {
        // 🔥 Thread Safety: Agar background thread se data aaye to postValue use karein
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            mediaList.setValue(list);
        } else {
            mediaList.postValue(list);
        }
    }

    public void updateDownloadStatus(String url, boolean status) {
        List<MediaItem> currentList = mediaList.getValue();
        if (currentList != null) {
            boolean isFound = false;

            // List ka snapshot lein taake loop ke doran data change na ho (Concurrent Modification safety)
            for (MediaItem item : currentList) {
                String matchUrl = item.isVideo() ? item.getVideoUrl() : item.getUrl();
                if (matchUrl != null && matchUrl.equals(url)) {
                    if (item.isDownloaded() != status) { // Sirf tab update karein agar status badla ho
                        item.setDownloaded(status);
                        isFound = true;
                    }
                }
            }

            if (isFound) {
                // postValue zaroori hai kyunke ye aksar background executor se call hota hai
                mediaList.postValue(currentList);
            }
        }
    }

    public void clearList() {
        mediaList.setValue(new ArrayList<>());
    }
}