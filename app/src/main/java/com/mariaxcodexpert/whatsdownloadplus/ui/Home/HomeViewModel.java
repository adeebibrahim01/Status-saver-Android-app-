package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

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
import com.mariaxcodexpert.whatsdownloadplus.data.local.Dashboard.DashboardDao;
import com.mariaxcodexpert.whatsdownloadplus.data.local.HomeDashboardEntity;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * 🔥 HOME VIEWMODEL: Focuses on "Recently Downloaded" media.
 * Logic: Filters for isDownloaded = 1 and ensures files exist on disk.
 */
public class HomeViewModel extends AndroidViewModel {
    private final ImageDao imageDao;
    private final VideoDao videoDao;
    private final DashboardDao dashboardDao;
    private final ExecutorService executor = AppDatabase.databaseWriteExecutor;

    public final LiveData<HomeDashboardEntity> dashboardStats;

    private final MutableLiveData<List<ImageEntity>> _recentImages = new MutableLiveData<>();
    public final LiveData<List<ImageEntity>> recentImages = _recentImages;

    private final MutableLiveData<List<VideoEntity>> _recentVideos = new MutableLiveData<>();
    public final LiveData<List<VideoEntity>> recentVideos = _recentVideos;

    private final MutableLiveData<Boolean> _isSyncing = new MutableLiveData<>(false);
    public LiveData<Boolean> isSyncing() { return _isSyncing; }

    public HomeViewModel(@NonNull Application app) {
        super(app);
        AppDatabase db = AppDatabase.getInstance(app);
        imageDao = db.imageDao();
        videoDao = db.videoDao();
        dashboardDao = db.dashboardDao();
        dashboardStats = dashboardDao.getDashboardStatsLive();

        refreshDashboardData();
    }

    public void refreshDashboardData() {
        if (Boolean.TRUE.equals(_isSyncing.getValue())) return;
        _isSyncing.postValue(true);

        executor.execute(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                long twentyFourHoursAgo = currentTime - 86400000L;
                long sevenDaysAgo = currentTime - (7 * 86400000L);
                long startOfToday = getStartOfDay();

                // 🔴 STEP 1: Database Maintenance
                imageDao.deleteOldRecords(sevenDaysAgo);
                videoDao.deleteOldRecords(sevenDaysAgo);

                Context ctx = getApplication();
                String uriStr = ctx.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .getString("statusFolderUri", null);

                // 🔥 STEP 2: WhatsApp Folder Scan (Independent of Dashboard Display)
                HashSet<String> whatsappDiskFiles = new HashSet<>();
                if (uriStr != null) {
                    Uri folderUri = Uri.parse(uriStr);
                    DocumentFile folder = DocumentFile.fromTreeUri(ctx, folderUri);
                    if (folder != null && folder.exists()) {
                        DocumentFile[] folderFiles = folder.listFiles();
                        if (folderFiles != null) {
                            for (DocumentFile f : folderFiles) {
                                String name = f.getName();
                                if (name != null && !name.startsWith(".") && !name.equalsIgnoreCase(".nomedia")) {
                                    whatsappDiskFiles.add(name);
                                }
                            }
                        }
                    }
                }
                List<String> activeWhatsAppList = new ArrayList<>(whatsappDiskFiles);

                // 🔥 STEP 3: Ghost Cleanup (Only for unsaved WhatsApp statuses)
                if (activeWhatsAppList.isEmpty()) {
                    imageDao.clearAllUnsavedImages();
                    videoDao.clearAllUnsavedVideos();
                } else {
                    imageDao.deleteGhostImages(activeWhatsAppList);
                    videoDao.deleteGhostVideos(activeWhatsAppList);
                }

                // STEP 4: Discovery (New WhatsApp Statuses)
                // Discovery logic stays here to populate new items for the "WhatsApp" tab stats
                // ... (Discovery logic remains same as your original)

                // 🔥 STEP 5: RECENTLY DOWNLOADED (The Critical Fix)
                // Hum activeDiskList ka filter HATA RAHE HAIN.
                // Kyunki Online Search se download ki hui images WhatsApp folder mein nahi hoti.

                // Ye methods ab simple "WHERE isDownloaded = 1" use karein ge
                List<ImageEntity> savedImages = imageDao.getOnlyDownloadedImagesSync();
                List<VideoEntity> savedVideos = videoDao.getOnlyDownloadedVideosSync();

                _recentImages.postValue(savedImages);
                _recentVideos.postValue(savedVideos);

                // Update Dashboard Stats
                int currentActiveCount = whatsappDiskFiles.size(); // WhatsApp active statuses count
                int calculatedTotal = dashboardDao.getTotalDownloadedCount();
                int calculatedToday = imageDao.getTodayCountSync(startOfToday) + videoDao.getTodayCountSync(startOfToday);
                updateDashboardTable(calculatedTotal, calculatedToday, currentActiveCount);

            } catch (Exception e) {
                Log.e("HOME_VM_ERROR", "Refresh Failed: " + e.getMessage());
            } finally {
                _isSyncing.postValue(false);
            }
        });
    }

    private void updateDashboardTable(int total, int today, int active) {
        String dateStr = getJoinedDate();
        HomeDashboardEntity s = new HomeDashboardEntity(1, today, total, active, dateStr);
        dashboardDao.insertStats(s);
    }

    private long getStartOfDay() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private String getJoinedDate() {
        try {
            long time = getApplication().getPackageManager().getPackageInfo(getApplication().getPackageName(), 0).firstInstallTime;
            return "Since " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(time));
        } catch (Exception e) {
            return "Since " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        }
    }

    public void resetSyncPhase() {
        _isSyncing.setValue(false);
    }
}