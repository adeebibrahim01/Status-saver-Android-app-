package com.mariaxcodexpert.whatsdownloadplus.ui.gallery;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.*;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.*;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.*;
import java.util.*;
import java.util.concurrent.*;

public class GalleryViewModel extends AndroidViewModel {

    public static class GalleryUiState {
        public final List<Object> data;
        public final boolean isLoading;
        public final boolean showEmpty;
        public final boolean isRefreshing;

        public GalleryUiState(List<Object> data, boolean isLoading, boolean showEmpty, boolean isRefreshing) {
            this.data = data;
            this.isLoading = isLoading;
            this.showEmpty = showEmpty;
            this.isRefreshing = isRefreshing;
        }
    }

    private final ImageDao imageDao;
    private final VideoDao videoDao;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private final MutableLiveData<GalleryUiState> _uiState = new MutableLiveData<>(
            new GalleryUiState(new ArrayList<>(), true, false, false)
    );
    public LiveData<GalleryUiState> getUiState() { return _uiState; }

    private boolean currentIsVideoTab = false;

    public GalleryViewModel(@NonNull Application app) {
        super(app);
        AppDatabase db = AppDatabase.getInstance(app);
        imageDao = db.imageDao();
        videoDao = db.videoDao();
    }

    public void loadStatuses(Context ctx, Uri uri, boolean isManual, boolean isVideoTab) {
        // 🔥 Agar tab change hua hai to foran list clear aur loading start
        boolean tabChanged = (this.currentIsVideoTab != isVideoTab);
        this.currentIsVideoTab = isVideoTab;

        if (uri == null) {
            _uiState.postValue(new GalleryUiState(new ArrayList<>(), false, true, false));
            return;
        }

        if (tabChanged) {
            // Clear list immediately so old tab's data doesn't stay on screen
            _uiState.setValue(new GalleryUiState(new ArrayList<>(), true, false, isManual));
        } else {
            // Just show swipe refresh if it's a manual refresh on same tab
            GalleryUiState current = _uiState.getValue();
            _uiState.setValue(new GalleryUiState(current != null ? current.data : new ArrayList<>(), !isManual, false, isManual));
        }

        executor.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                DocumentFile folder = DocumentFile.fromTreeUri(ctx, uri);

                if (folder != null && folder.exists()) {
                    List<ImageEntity> newImgs = new ArrayList<>();
                    List<VideoEntity> newVids = new ArrayList<>();
                    DocumentFile[] files = folder.listFiles();

                    if (files != null) {
                        for (DocumentFile f : files) {
                            String name = f.getName();
                            if (name == null || name.startsWith(".") || name.equals(".nomedia")) continue;

                            long time = f.lastModified();
                            long expiry = time + 86400000L;
                            if (now > expiry) continue;

                            String ext = name.toLowerCase();
                            if (ext.endsWith(".mp4") || ext.endsWith(".mkv")) {
                                if (!videoDao.isVideoExists(name)) {
                                    newVids.add(new VideoEntity(name, f.getUri().toString(), "", time, false, expiry));
                                }
                            } else if (ext.endsWith(".jpg") || ext.endsWith(".png") || ext.endsWith(".webp") || ext.endsWith(".jpeg")) {
                                if (!imageDao.isImageExists(name)) {
                                    newImgs.add(new ImageEntity(name, f.getUri().toString(), "", time, false, expiry));
                                }
                            }
                        }
                    }
                    if (!newImgs.isEmpty()) imageDao.insertImages(newImgs);
                    if (!newVids.isEmpty()) videoDao.insertVideos(newVids);
                }
                refreshLocalData();
            } catch (Exception e) {
                Log.e("GalleryVM", "Sync Error: " + e.getMessage());
                refreshLocalData();
            }
        });
    }

    private void refreshLocalData() {
        executor.execute(() -> {
            try {
                Context ctx = getApplication();
                String uriStr = ctx.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .getString("statusFolderUri", null);

                if (uriStr == null) return;

                Uri folderUri = Uri.parse(uriStr);
                DocumentFile folder = DocumentFile.fromTreeUri(ctx, folderUri);

                List<String> activeDiskList = new ArrayList<>();
                if (folder != null && folder.exists()) {
                    DocumentFile[] files = folder.listFiles();
                    if (files != null) {
                        for (DocumentFile f : files) {
                            if (f.getName() != null) activeDiskList.add(f.getName());
                        }
                    }
                }

                List<Object> combinedData = new ArrayList<>();
                if (currentIsVideoTab) {
                    combinedData.addAll(videoDao.getActiveVideosSync(activeDiskList));
                } else {
                    combinedData.addAll(imageDao.getActiveImagesSync(activeDiskList));
                }

                _uiState.postValue(new GalleryUiState(combinedData, false, combinedData.isEmpty(), false));
            } catch (Exception e) {
                Log.e("GALLERY_VM", "Refresh failed", e);
            }
        });
    }

    public void markImageDownloaded(ImageEntity item, String path) {
        executor.execute(() -> {
            imageDao.updateImageDownloadStatus(item.fileName, true, path, System.currentTimeMillis());
            refreshLocalData();
        });
    }

    public void markVideoDownloaded(VideoEntity item, String path) {
        executor.execute(() -> {
            videoDao.updateVideoDownloadStatus(item.fileName, true, path, System.currentTimeMillis());
            refreshLocalData();
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}