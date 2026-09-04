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

    private final Map<Boolean, List<Object>> tabCache = new ConcurrentHashMap<>();

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

        tabCache.put(true, new ArrayList<>());
        tabCache.put(false, new ArrayList<>());
    }

    public void loadStatuses(Uri uri, boolean isManual, boolean isVideoTab) {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            Log.e("GalleryVM", "Executor is shutdown, loadStatuses skipped.");
            return;
        }

        boolean tabChanged = (this.currentIsVideoTab != isVideoTab);
        this.currentIsVideoTab = isVideoTab;

        if (uri == null) {
            _uiState.postValue(new GalleryUiState(new ArrayList<>(), false, true, false));
            return;
        }

        List<Object> cachedData = tabCache.get(isVideoTab);
        if (cachedData == null) cachedData = new ArrayList<>();

        if (tabChanged) {
            _uiState.setValue(new GalleryUiState(cachedData, cachedData.isEmpty(), false, isManual));
        } else {
            GalleryUiState current = _uiState.getValue();
            _uiState.setValue(new GalleryUiState(current != null ? current.data : cachedData, !isManual, false, isManual));
        }

        try {
            executor.execute(() -> {
                try {
                    Context ctx = getApplication();
                    long now = System.currentTimeMillis();
                    DocumentFile folder = DocumentFile.fromTreeUri(ctx, uri);

                    if (folder != null && folder.exists()) {
                        List<ImageEntity> newImgs = new ArrayList<>();
                        List<VideoEntity> newVids = new ArrayList<>();
                        DocumentFile[] files = folder.listFiles();

                        if (files != null) {
                            for (DocumentFile f : files) {
                                try {
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
                                } catch (Exception e) {
                                    Log.e("GalleryVM", "File processing skipped: " + e.getMessage());
                                }
                            }
                        }

                        if (!newImgs.isEmpty()) imageDao.insertImages(newImgs);
                        if (!newVids.isEmpty()) videoDao.insertVideos(newVids);
                    }
                    refreshLocalData();
                } catch (Exception e) {
                    Log.e("GalleryVM", "Sync Critical Error", e);
                    refreshLocalData();
                }
            });
        } catch (RejectedExecutionException e) {
            Log.e("GalleryVM", "Task rejected in loadStatuses: " + e.getMessage(), e);
        }
    }

    private void refreshLocalData() {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) return;
        try {
            executor.execute(() -> {
                try {
                    Context ctx = getApplication();
                    if (ctx == null) return;

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
                                String name = f.getName();
                                if (name != null) activeDiskList.add(name);
                            }
                        }
                    }

                    boolean currentTab = this.currentIsVideoTab;
                    List<Object> combinedData = new ArrayList<>();
                    if (currentTab) {
                        combinedData.addAll(videoDao.getActiveVideosSync(activeDiskList));
                    } else {
                        combinedData.addAll(imageDao.getActiveImagesSync(activeDiskList));
                    }

                    tabCache.put(currentTab, combinedData);

                    if (this.currentIsVideoTab == currentTab) {
                        _uiState.postValue(new GalleryUiState(combinedData, false, combinedData.isEmpty(), false));
                    }
                } catch (Exception e) {
                    Log.e("GALLERY_VM", "Refresh UI failed", e);
                    _uiState.postValue(new GalleryUiState(new ArrayList<>(), false, true, false));
                }
            });
        } catch (RejectedExecutionException e) {
            Log.e("GalleryVM", "Task rejected in refreshLocalData: " + e.getMessage(), e);
        }
    }

    public void markImageDownloaded(ImageEntity item, String path) {
        if (item == null || path == null) return;
        if (executor == null || executor.isShutdown() || executor.isTerminated()) return;
        try {
            executor.execute(() -> {
                try {
                    imageDao.updateImageDownloadStatus(item.fileName, true, path, System.currentTimeMillis());
                    refreshLocalData();
                } catch (Exception e) {
                    Log.e("DB_UPDATE", "Image update failed", e);
                }
            });
        } catch (RejectedExecutionException e) {
            Log.e("GalleryVM", "Task rejected in markImageDownloaded: " + e.getMessage(), e);
        }
    }

    public void markVideoDownloaded(VideoEntity item, String path) {
        if (item == null || path == null) return;
        if (executor == null || executor.isShutdown() || executor.isTerminated()) return;
        try {
            executor.execute(() -> {
                try {
                    videoDao.updateVideoDownloadStatus(item.fileName, true, path, System.currentTimeMillis());
                    refreshLocalData();
                } catch (Exception e) {
                    Log.e("DB_UPDATE", "Video update failed", e);
                }
            });
        } catch (RejectedExecutionException e) {
            Log.e("GalleryVM", "Task rejected in markVideoDownloaded: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor != null) {
            try {
                executor.shutdownNow();
            } catch (Exception e) {
                Log.e("GalleryVM", "Error shutting down executor: " + e.getMessage(), e);
            }
        }
    }
}