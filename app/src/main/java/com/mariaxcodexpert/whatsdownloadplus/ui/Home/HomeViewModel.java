package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.*;

import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.*;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.*;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Dashboard.DashboardDao;
import com.mariaxcodexpert.whatsdownloadplus.data.local.HomeDashboardEntity;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomeViewModel extends AndroidViewModel {

    public static class HomeUiState {
        public final List<Object> combinedList;
        public final HomeDashboardEntity stats;
        public final boolean isSyncing;

        public HomeUiState(List<Object> combinedList, HomeDashboardEntity stats, boolean isSyncing) {
            this.combinedList = (combinedList != null) ? combinedList : new ArrayList<>();
            this.stats = stats;
            this.isSyncing = isSyncing;
        }

        public boolean isSameAs(HomeUiState other) {
            if (other == null) return false;
            if (this.isSyncing != other.isSyncing) return false;

            if (this.stats != null && other.stats != null) {
                if (this.stats.todayCount != other.stats.todayCount ||
                        this.stats.totalCount != other.stats.totalCount ||
                        this.stats.activeStatuses != other.stats.activeStatuses) return false;
            } else if (this.stats != other.stats) return false;

            return this.combinedList.size() == other.combinedList.size();
        }
    }

    private final ImageDao imageDao;
    private final VideoDao videoDao;
    private final DashboardDao dashboardDao;
    private final ExecutorService executor = AppDatabase.databaseWriteExecutor;
    private final AtomicBoolean isSyncingFlag = new AtomicBoolean(false);

    private final MutableLiveData<HomeUiState> _uiState = new MutableLiveData<>();
    public LiveData<HomeUiState> uiState = _uiState;

    public HomeViewModel(@NonNull Application app) {
        super(app);
        AppDatabase db = AppDatabase.getInstance(app);
        imageDao = db.imageDao();
        videoDao = db.videoDao();
        dashboardDao = db.dashboardDao();

        executor.execute(() -> {
            try {
                HomeUiState initialState = new HomeUiState(getCombinedMediaSync(), dashboardDao.getDashboardStatsSync(), false);
                _uiState.postValue(initialState);
                refreshDashboardData();
            } catch (Exception e) {
                Log.e("HomeVM", "Initial Load Error", e);
            }
        });
    }

    public void refreshDashboardData() {
        if (!isSyncingFlag.compareAndSet(false, true)) return;

        executor.execute(() -> {
            try {
                Context context = getApplication();
                Set<String> diskFiles = new HashSet<>();

                String uriStr = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .getString("statusFolderUri", null);

                if (uriStr != null) {
                    try {
                        DocumentFile folder = DocumentFile.fromTreeUri(context, Uri.parse(uriStr));
                        if (folder != null && folder.exists()) {
                            DocumentFile[] files = folder.listFiles();
                            if (files != null) {
                                for (DocumentFile f : files) {
                                    String name = f.getName();
                                    if (name != null && !name.startsWith(".") && !name.equals(".nomedia")) {
                                        diskFiles.add(name);
                                    }
                                }
                            }
                        }
                    } catch (Exception folderEx) {
                        Log.e("HomeVM", "Folder Scan Failed", folderEx);
                    }
                }

                DatabaseCleaner.performSilentCleanup(context, imageDao, videoDao, new ArrayList<>(diskFiles));

                List<Object> combined = getCombinedMediaSync();

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long dayStart = cal.getTimeInMillis();

                int total = dashboardDao.getTotalDownloadedCount();
                int today = imageDao.getTodayCountSync(dayStart) + videoDao.getTodayCountSync(dayStart);

                HomeDashboardEntity newStats = new HomeDashboardEntity(1, today, total, diskFiles.size(), getJoinedDate());

                HomeUiState newState = new HomeUiState(combined, newStats, false);
                HomeUiState currentState = _uiState.getValue();

                if (currentState == null || !newState.isSameAs(currentState)) {
                    _uiState.postValue(newState);
                } else {
                    _uiState.postValue(new HomeUiState(currentState.combinedList, currentState.stats, false));
                }

                dashboardDao.insertStats(newStats);

            } catch (Exception e) {
                Log.e("HomeVM", "Refresh Final Error", e);
            } finally {
                isSyncingFlag.set(false);
            }
        });
    }

    private List<Object> getCombinedMediaSync() {
        List<Object> combined = new ArrayList<>();
        try {
            List<ImageEntity> img = imageDao.getOnlyDownloadedImagesSync();
            List<VideoEntity> vid = videoDao.getOnlyDownloadedVideosSync();

            if (img != null) combined.addAll(img);
            if (vid != null) combined.addAll(vid);

            if (combined.size() > 1) {
                Collections.sort(combined, (o1, o2) -> Long.compare(getTs(o2), getTs(o1)));
            }
        } catch (Exception e) {
            Log.e("HomeVM", "Combined Sort Error", e);
        }
        return combined;
    }

    private long getTs(Object o) {
        if (o instanceof ImageEntity) return ((ImageEntity) o).lastModified;
        if (o instanceof VideoEntity) return ((VideoEntity) o).lastModified;
        return 0L;
    }

    private String getJoinedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        try {
            Context ctx = getApplication();
            long time = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).firstInstallTime;
            return ctx.getString(R.string.joined_since_date, sdf.format(new Date(time)));
        } catch (Exception e) {
            Context ctx = getApplication();
            return ctx.getString(R.string.joined_since_date, sdf.format(new Date()));
        }
    }
}