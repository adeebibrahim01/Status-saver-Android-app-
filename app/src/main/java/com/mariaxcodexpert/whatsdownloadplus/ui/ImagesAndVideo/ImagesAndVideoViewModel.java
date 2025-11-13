package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ImagesAndVideoViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ImagesAndVideoViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gallery fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}