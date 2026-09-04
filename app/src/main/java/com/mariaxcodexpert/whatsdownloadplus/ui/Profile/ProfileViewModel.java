package com.mariaxcodexpert.whatsdownloadplus.ui.Profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ProfileEntity.ProfileEntity;

public class ProfileViewModel extends AndroidViewModel {
    private final AppDatabase db;
    private final FirebaseAuth auth;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.db = AppDatabase.getInstance(application);
        this.auth = FirebaseAuth.getInstance();
    }

    public LiveData<ProfileEntity> getProfileLiveData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            return db.profileDao().getProfileLiveData(currentUser.getUid());
        }
        return new MutableLiveData<>(null);
    }
    public ProfileEntity getProfile(String uid) {
        return db.profileDao().getProfile(uid);
    }
    public String getCurrentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }
}