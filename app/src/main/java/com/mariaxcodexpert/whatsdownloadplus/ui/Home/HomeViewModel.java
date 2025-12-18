package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Date;

public class HomeViewModel extends AndroidViewModel {

    private final MutableLiveData<String> joinedDate = new MutableLiveData<>();
    private final MutableLiveData<String> toolbarTitle = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        loadJoinedDate();
        toolbarTitle.setValue("Home");
    }

    private void loadJoinedDate() {
        try {
            PackageInfo packageInfo = getApplication()
                    .getPackageManager()
                    .getPackageInfo(getApplication().getPackageName(), 0);

            long firstInstallTime = packageInfo.firstInstallTime;
            String formattedDate = DateFormat.format("MMMM dd, yyyy", new Date(firstInstallTime)).toString();
            joinedDate.setValue("\uD83C\uDF89 Joined on " + formattedDate + " ");
        } catch (PackageManager.NameNotFoundException e) {
            joinedDate.setValue("\uD83C\uDF89 Joined");
        }
    }

    // Expose LiveData
    public LiveData<String> getJoinedDate() {
        return joinedDate;
    }

    public LiveData<String> getToolbarTitle() {
        return toolbarTitle;
    }

    // Update toolbar title dynamically (Images/Videos/Download)
    public void setToolbarTitle(String title) {
        toolbarTitle.setValue(title);
    }
}
