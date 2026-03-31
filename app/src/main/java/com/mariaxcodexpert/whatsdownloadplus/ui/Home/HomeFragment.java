package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mariaxcodexpert.whatsdownloadplus.LoadingHandler;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.VersionHelper;
import com.mariaxcodexpert.whatsdownloadplus.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private NavController navController;
    private HomeViewModel viewModel;

    private static final int MAX_ITEMS = 10;

    // 🔥 PROFESSIONAL CONFIGURATION: Use your exact app folder name here
    private static final String DOWNLOAD_FOLDER_NAME = "Status Saver";
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";

    private RecentDownloadsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(this);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupClickListeners();
        observeViewModel();
        setupRecyclerView();

        // Initial Data Load
        refreshAllData();

        // App Version Setup
        VersionHelper versionHelper = new VersionHelper(requireContext());
        binding.projectVersion.setText(versionHelper.getAppVersion());
        requireActivity().setTitle("Home");

        handleIntentExtras();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        binding.rvRecentDownloads.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvRecentDownloads.setHasFixedSize(true);

        // Load Initial Media for Adapter
        List<MediaItem> recentItems = getRecentMediaFromMediaStore();
        adapter = new RecentDownloadsAdapter(recentItems, binding.tvRecentDownloadsEmpty);
        binding.rvRecentDownloads.setAdapter(adapter);
    }

    // --- MEDIA FETCHING LOGIC ---

    private List<MediaItem> getRecentMediaFromMediaStore() {
        List<MediaItem> result = new ArrayList<>();
        Context context = getContext();
        if (context == null) return result;

        fetchMedia(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, result);
        fetchMedia(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, result);

        Collections.sort(result, (a, b) -> Long.compare(b.dateAdded, a.dateAdded));

        if (result.size() > MAX_ITEMS) {
            return new ArrayList<>(result.subList(0, MAX_ITEMS));
        }
        return result;
    }

    private void fetchMedia(Context context, Uri uri, boolean isVideo, List<MediaItem> out) {
        // Optimization: Filter by Folder Path to ensure only app downloads show
        String selection = MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?";
        String[] args = { "%" + DOWNLOAD_FOLDER_NAME + "%" };

        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED},
                selection, args, MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);
                while (cursor.moveToNext() && out.size() < MAX_ITEMS * 2) {
                    long id = cursor.getLong(idCol);
                    long date = cursor.getLong(dateCol) * 1000L;
                    out.add(new MediaItem(ContentUris.withAppendedId(uri, id), isVideo, date));
                }
            }
        } catch (Exception e) { Log.e("HomeFragment", "FetchMedia Error", e); }
    }

    // --- STATS LOGIC ---

    private void updateDownloadsStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Context context = getContext();
            if (context == null) return;

            // 1. Today's Count
            Calendar calToday = Calendar.getInstance();
            zeroTime(calToday);
            long todayTimestamp = calToday.getTimeInMillis() / 1000;
            int todayCount = getCountFromMediaStore(context, todayTimestamp);

            // 2. Total Lifetime Count (All time)
            int totalLifetimeCount = getCountFromMediaStore(context, 0);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.tvTodayCount.setText(String.valueOf(todayCount));
                        binding.tvLast7DaysCount.setText(String.valueOf(totalLifetimeCount));
                    }
                });
            }
        });
    }

    private int getCountFromMediaStore(Context context, long afterTimestamp) {
        int count = 0;
        Uri collection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL) :
                MediaStore.Files.getContentUri("external");

        // FIX: Consolidated query to count only Images/Videos in the specific folder
        String selection = "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE +
                " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ")" +
                " AND " + MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?" +
                " AND " + MediaStore.MediaColumns.DATE_ADDED + " >= ?";

        String[] selectionArgs = new String[]{"%" + DOWNLOAD_FOLDER_NAME + "%", String.valueOf(afterTimestamp)};

        try (Cursor cursor = context.getContentResolver().query(collection,
                new String[]{MediaStore.MediaColumns._ID}, selection, selectionArgs, null)) {
            if (cursor != null) {
                count = cursor.getCount();
            }
        } catch (Exception e) { Log.e("HomeFragment", "Stats Query Error", e); }
        return count;
    }

    private void updateStatusCount() {
        Executors.newSingleThreadExecutor().execute(() -> {
            int totalCount = 0;
            Context context = getContext();
            if (context == null) return;

            // Using Splash_screen persistent keys
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String folderUriStr = prefs.getString(KEY_STATUS_FOLDER_URI, null);

            if (folderUriStr != null && !folderUriStr.isEmpty()) {
                try {
                    Uri folderUri = Uri.parse(folderUriStr);
                    DocumentFile pickedDir = DocumentFile.fromTreeUri(context, folderUri);

                    if (pickedDir != null && pickedDir.exists() && pickedDir.isDirectory()) {
                        DocumentFile[] allFiles = pickedDir.listFiles();
                        if (allFiles != null) {
                            for (DocumentFile file : allFiles) {
                                if (file.isFile()) {
                                    String mimeType = file.getType();
                                    if (mimeType != null && (mimeType.startsWith("image/") || mimeType.startsWith("video/"))) {
                                        totalCount++;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) { Log.e("StatusError", "Scanning error", e); }
            }

            final int finalCount = totalCount;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.tvActiveStreak.setText(String.valueOf(finalCount));
                        String label = (finalCount <= 1) ? "All Status" : "All Statuses";
                        binding.tvStatusLabel.setText(label);
                    }
                });
            }
        });
    }

    // --- HELPER METHODS ---

    private void zeroTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private void setupClickListeners() {
        binding.btnImages.setOnClickListener(v -> openGallery(false));
        binding.btnVideos.setOnClickListener(v -> openGallery(true));
        binding.btnSaved.setOnClickListener(v -> navigateToDownload());
    }

    private void openGallery(boolean showVideos) {
        LoadingHandler.showLoading(binding.getRoot(), "Opening Gallery...", 800, () -> {
            Bundle args = new Bundle();
            args.putBoolean("showVideos", showVideos);
            navController.navigate(R.id.nav_gallery, args);
        });
    }
    private void navigateToDownload() {
        navController.navigate(R.id.nav_download);
    }

    private void observeViewModel() {
        viewModel.getJoinedDate().observe(getViewLifecycleOwner(), joinedText -> {
            if (binding != null) binding.joinedText.setText(joinedText);
        });
    }

    private void handleIntentExtras() {
        Intent intent = requireActivity().getIntent();
        if (intent != null && intent.hasExtra("openFragment")) {
            String fragmentToOpen = intent.getStringExtra("openFragment");
            if ("ImagesAndVideo".equals(fragmentToOpen)) {
                openGallery(intent.getBooleanExtra("isVideo", false));
                intent.removeExtra("openFragment");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAllData();
    }

    private void refreshAllData() {
        updateStatusCount();
        updateDownloadsStats();
        List<MediaItem> recentItems = getRecentMediaFromMediaStore();
        if (adapter != null) {
            adapter.updateData(recentItems);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}