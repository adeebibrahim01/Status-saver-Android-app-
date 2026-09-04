package com.mariaxcodexpert.whatsdownloadplus.data.local.ProfileEntity;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProfile(ProfileEntity profile);


    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    LiveData<ProfileEntity> getProfileLiveData(String uid);

    @Query("SELECT EXISTS(SELECT 1 FROM user_profile LIMIT 1)")
    boolean isUserExists();

    @Query("SELECT uid FROM user_profile LIMIT 1")
    String getFirstUserUid();

    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    ProfileEntity getProfile(String uid);

}