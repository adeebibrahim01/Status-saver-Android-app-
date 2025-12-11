package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import androidx.documentfile.provider.DocumentFile;
import java.util.List;

public class ImagesAndVideoViewModel extends ViewModel {

    private final MutableLiveData<List<DocumentFile>> imagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<DocumentFile>> videosLiveData = new MutableLiveData<>();

    public LiveData<List<DocumentFile>> getImages() {
        return imagesLiveData;
    }

    public LiveData<List<DocumentFile>> getVideos() {
        return videosLiveData;
    }

    public void setImages(List<DocumentFile> images) {
        imagesLiveData.setValue(images);
    }

    public void setVideos(List<DocumentFile> videos) {
        videosLiveData.setValue(videos);
    }
}
