package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.*;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class DownloadViewModel extends AndroidViewModel {

    public static class DownloadUiState {
        public final List<Object> data;
        public final boolean isLoading;
        public DownloadUiState(List<Object> d, boolean l) { this.data = d; this.isLoading = l; }
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

    public DownloadViewModel(@NonNull Application app) {
        super(app);
        AppDatabase db = AppDatabase.getInstance(app);
        imageDao = db.imageDao();
        videoDao = db.videoDao();
        pool = AppDatabase.databaseWriteExecutor;

        _uiState.setValue(new DownloadUiState(new ArrayList<>(), true));
        setupStateObserver();
        performAutoCleanup();
    }

    private void setupStateObserver() {
        LiveData<List<ImageEntity>> imgLive = imageDao.getSavedImages();
        LiveData<List<VideoEntity>> vidLive = videoDao.getSavedVideos();

        _uiState.addSource(imgLive, imgs -> combine(imgs, vidLive.getValue()));
        _uiState.addSource(vidLive, vids -> combine(imgLive.getValue(), vids));
    }

    private void combine(List<ImageEntity> imgs, List<VideoEntity> vids) {
        pool.execute(() -> {
            try {
                List<Object> list = new ArrayList<>();
                if (imgs != null) list.addAll(imgs);
                if (vids != null) list.addAll(vids);

                if (!list.isEmpty()) {
                    Collections.sort(list, (o1, o2) -> {
                        long t1 = (o1 instanceof ImageEntity) ? ((ImageEntity) o1).lastModified : ((VideoEntity) o1).lastModified;
                        long t2 = (o2 instanceof ImageEntity) ? ((ImageEntity) o2).lastModified : ((VideoEntity) o2).lastModified;
                        return Long.compare(t2, t1); // Newest first
                    });
                }

                _uiState.postValue(new DownloadUiState(list, false));
            } catch (Exception e) {
                Log.e("COMBINE_ERR", "Sorting failed: " + e.getMessage());
                _uiState.postValue(new DownloadUiState(new ArrayList<>(), false));
            }
        });
    }

    public void refreshSavedFiles() {
        pool.execute(() -> {
            try {
                List<ImageEntity> images = imageDao.getAllImagesSync();
                List<VideoEntity> videos = videoDao.getAllVideosSync();

                if (images != null) syncEntities(images);
                if (videos != null) syncEntities(videos);

            } catch (Exception e) {
                Log.e("SYNC_ERR", "Sync failed: " + e.getMessage());
            }
        });
    }

    private void syncEntities(List<?> entities) {
        if (entities == null || entities.isEmpty()) return;
        for (Object e : entities) {
            try {
                String path = (e instanceof ImageEntity) ? ((ImageEntity) e).gallery_path : ((VideoEntity) e).gallery_path;
                boolean isDown = (e instanceof ImageEntity) ? ((ImageEntity) e).isDownloaded : ((VideoEntity) e).isDownloaded;

                if (isDown && isFileMissing(path)) {
                    performDbReset(e);
                }
            } catch (Exception err) {
                continue;
            }
        }
    }



    private boolean isFileMissing(String path) {
        if (path == null || path.isEmpty()) return true;
        try {
            Uri uri = Uri.parse(path);
            try (Cursor c = getApplication().getContentResolver().query(uri,
                    new String[]{MediaStore.MediaColumns._ID}, null, null, null)) {
                return c == null || !c.moveToFirst();
            }
        } catch (Exception e) {
            Log.e("FILE_CHECK", "Error checking file existence: " + e.getMessage());
            return true;
        }
    }

    public void deleteFile(Object item) {
        if (item == null) return;
        this.pendingDeleteItem = item;

        String path = (item instanceof ImageEntity) ? ((ImageEntity) item).gallery_path : ((VideoEntity) item).gallery_path;

        if (path == null || path.isEmpty()) {
            performDbReset(item);
            return;
        }

        try {
            Uri uri = Uri.parse(path);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PendingIntent pi = MediaStore.createDeleteRequest(getApplication().getContentResolver(), Collections.singletonList(uri));
                _permissionIntent.postValue(pi);
            } else {
                pool.execute(() -> {
                    try {
                        int deleted = getApplication().getContentResolver().delete(uri, null, null);
                        if (deleted > 0 || isFileMissing(path)) {
                            performDbReset(item);
                            pendingDeleteItem = null;
                        }
                    } catch (SecurityException se) {
                        handleSecurity(se);
                    } catch (Exception e) {
                        Log.e("DELETE_ERR", "Legacy delete failed: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            Log.e("DELETE_URI_ERR", "Invalid URI path: " + path);
            performDbReset(item);
        }
    }
    private void handleSecurity(SecurityException e) {
        RecoverableSecurityException rse = (e instanceof RecoverableSecurityException) ? (RecoverableSecurityException) e :
                (e.getCause() instanceof RecoverableSecurityException ? (RecoverableSecurityException) e.getCause() : null);
        if (rse != null) _permissionIntent.postValue(rse.getUserAction().getActionIntent());
    }

    public void completePendingDelete() {
        if (pendingDeleteItem != null) pool.execute(() -> { performDbReset(pendingDeleteItem); pendingDeleteItem = null; });
    }

    private void performDbReset(Object item) {
        if (item == null) return;

        pool.execute(() -> {
            try {
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
            } catch (Exception e) {
                Log.e("DB_RESET_ERR", "Database update failed: " + e.getMessage());
            }
        });
    }

    public void performAutoCleanup() {
        SharedPreferences prefs = getApplication().getSharedPreferences("App_Settings", Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (!today.equals(prefs.getString("last_db_cleanup", ""))) {
            pool.execute(() -> {
                long now = System.currentTimeMillis();
                imageDao.cleanupExpiredImages(now);
                videoDao.cleanupExpiredVideos(now);
                prefs.edit().putString("last_db_cleanup", today).apply();
            });
        }
    }

    public void clearPermissionIntent() { _permissionIntent.postValue(null); }
}