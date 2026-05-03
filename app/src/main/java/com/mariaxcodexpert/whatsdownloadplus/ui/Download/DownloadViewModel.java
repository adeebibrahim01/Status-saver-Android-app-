package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.app.Application;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageDao;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoDao;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

public class DownloadViewModel extends AndroidViewModel {

    public static class DownloadUiState {
        public final List<Object> data;
        public final boolean isLoading;
        public final String errorMessage;

        public DownloadUiState(List<Object> data, boolean isLoading, String errorMessage) {
            this.data = data;
            this.isLoading = isLoading;
            this.errorMessage = errorMessage;
        }
    }

    private final ImageDao imageDao;
    private final VideoDao videoDao;
    private final ExecutorService pool;

    private Object pendingDeleteItem;

    private final MediatorLiveData<DownloadUiState> _uiState = new MediatorLiveData<>();
    public final LiveData<DownloadUiState> uiState = _uiState;

    private final MutableLiveData<PendingIntent> _permissionIntent = new MutableLiveData<>();
    public final LiveData<PendingIntent> permissionIntent = _permissionIntent;

    private final MutableLiveData<Boolean> _deleteSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> deleteSuccess = _deleteSuccess;

    public DownloadViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        this.imageDao = db.imageDao();
        this.videoDao = db.videoDao();
        this.pool = AppDatabase.databaseWriteExecutor;

        // Start with loading true to prevent initial jump
        _uiState.setValue(new DownloadUiState(new ArrayList<>(), true, null));

        setupStateObserver();
        performAutoCleanup();
    }

    private void setupStateObserver() {
        LiveData<List<ImageEntity>> imagesLive = imageDao.getSavedImages();
        LiveData<List<VideoEntity>> videosLive = videoDao.getSavedVideos();

        _uiState.addSource(imagesLive, images -> combineAndSort(images, videosLive.getValue()));
        _uiState.addSource(videosLive, videos -> combineAndSort(imagesLive.getValue(), videos));
    }

    private void combineAndSort(List<ImageEntity> images, List<VideoEntity> videos) {
        pool.execute(() -> {
            List<Object> combined = new ArrayList<>();
            if (images != null) combined.addAll(images);
            if (videos != null) combined.addAll(videos);

            if (!combined.isEmpty()) {
                try {
                    Collections.sort(combined, (o1, o2) -> {
                        long t1 = 0;
                        long t2 = 0;

                        // Crash Fix: Explicit type checking before casting
                        if (o1 instanceof ImageEntity) t1 = ((ImageEntity) o1).lastModified;
                        else if (o1 instanceof VideoEntity) t1 = ((VideoEntity) o1).lastModified;

                        if (o2 instanceof ImageEntity) t2 = ((ImageEntity) o2).lastModified;
                        else if (o2 instanceof VideoEntity) t2 = ((VideoEntity) o2).lastModified;

                        return Long.compare(t2, t1); // Newest first
                    });
                } catch (Exception e) {
                    Log.e("SORT_ERROR", "Comparator failed: " + e.getMessage());
                }
            }

            // Anti-Blink: Only post if data has actually changed
            DownloadUiState current = _uiState.getValue();
            if (current != null && current.data.equals(combined) && !current.isLoading) {
                return;
            }

            _uiState.postValue(new DownloadUiState(combined, false, null));
        });
    }

    public void refreshSavedFiles() {
        pool.execute(() -> {
            try {
                List<ImageEntity> savedImages = imageDao.getAllImagesSync();
                if (savedImages != null) {
                    for (ImageEntity img : savedImages) {
                        if (img.isDownloaded && isGalleryFileMissing(img.gallery_path)) {
                            performDbReset(img);
                        }
                    }
                }

                List<VideoEntity> savedVideos = videoDao.getAllVideosSync();
                if (savedVideos != null) {
                    for (VideoEntity vid : savedVideos) {
                        if (vid.isDownloaded && isGalleryFileMissing(vid.gallery_path)) {
                            performDbReset(vid);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("SYNC_ERROR", "Refresh failed: " + e.getMessage());
            }
        });
    }

    private boolean isGalleryFileMissing(String path) {
        if (path == null || path.isEmpty()) return true;
        try {
            Uri fileUri = Uri.parse(path);
            try (Cursor cursor = getApplication().getContentResolver().query(
                    fileUri, new String[]{MediaStore.MediaColumns._ID}, null, null, null)) {
                return cursor == null || !cursor.moveToFirst();
            }
        } catch (Exception e) {
            return true;
        }
    }

    public void deleteFile(Object item) {
        this.pendingDeleteItem = item;
        String path;
        if (item instanceof ImageEntity) path = ((ImageEntity) item).gallery_path;
        else if (item instanceof VideoEntity) path = ((VideoEntity) item).gallery_path;
        else {
            path = "";
        }

        if (path == null || path.isEmpty()) {
            performDbReset(item);
            return;
        }

        Uri mediaStoreUri = Uri.parse(path);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            List<Uri> uris = Collections.singletonList(mediaStoreUri);
            PendingIntent pi = MediaStore.createDeleteRequest(getApplication().getContentResolver(), uris);
            _permissionIntent.postValue(pi);
        } else {
            pool.execute(() -> {
                try {
                    int deleted = getApplication().getContentResolver().delete(mediaStoreUri, null, null);
                    if (deleted > 0 || isGalleryFileMissing(path)) {
                        performDbReset(item);
                        pendingDeleteItem = null;
                    }
                } catch (SecurityException securityException) {
                    handleSecurityException(securityException);
                } catch (Exception e) {
                    Log.e("DELETE_ERROR", "Legacy delete failed: " + e.getMessage());
                }
            });
        }
    }

    private void handleSecurityException(SecurityException e) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RecoverableSecurityException recoverable = null;
            if (e instanceof RecoverableSecurityException) {
                recoverable = (RecoverableSecurityException) e;
            } else if (e.getCause() instanceof RecoverableSecurityException) {
                recoverable = (RecoverableSecurityException) e.getCause();
            }

            if (recoverable != null) {
                _permissionIntent.postValue(recoverable.getUserAction().getActionIntent());
            }
        }
    }

    public void completePendingDelete() {
        if (pendingDeleteItem != null) {
            pool.execute(() -> {
                performDbReset(pendingDeleteItem);
                pendingDeleteItem = null;
            });
        }
    }

    private void performDbReset(Object item) {
        if (item instanceof ImageEntity) {
            ImageEntity img = (ImageEntity) item;
            img.isDownloaded = false;
            img.gallery_path = "";
            img.downloadTime = 0;
            imageDao.updateImage(img);
        } else if (item instanceof VideoEntity) {
            VideoEntity vid = (VideoEntity) item;
            vid.isDownloaded = false;
            vid.gallery_path = "";
            vid.downloadTime = 0;
            videoDao.updateVideo(vid);
        }
        _deleteSuccess.postValue(true);
    }

    public void performAutoCleanup() {
        SharedPreferences prefs = getApplication().getSharedPreferences("App_Settings", Context.MODE_PRIVATE);
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastCleanupDate = prefs.getString("last_db_cleanup", "");

        if (!todayDate.equals(lastCleanupDate)) {
            pool.execute(() -> {
                try {
                    long now = System.currentTimeMillis();
                    imageDao.cleanupExpiredImages(now);
                    videoDao.cleanupExpiredVideos(now);
                    prefs.edit().putString("last_db_cleanup", todayDate).apply();
                } catch (Exception e) {
                    Log.e("CLEANUP_ERROR", "Cleanup failed: " + e.getMessage());
                }
            });
        }
    }

    public void clearPermissionIntent() { _permissionIntent.postValue(null); }
}