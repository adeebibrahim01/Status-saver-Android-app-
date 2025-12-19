package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DownloadViewModel extends ViewModel {

    // Fragment text (optional)
    private final MutableLiveData<String> mText = new MutableLiveData<>();

    // ✅ Toolbar title LiveData
    private final MutableLiveData<String> toolbarTitle = new MutableLiveData<>();

    public DownloadViewModel() {
        // Directly set app name here
        toolbarTitle.setValue("WhatsDownload Plus");
    }

    public LiveData<String> getText() {
        return mText;
    }

}
