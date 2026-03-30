package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.card.MaterialCardView;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.VersionHelper;
import com.mariaxcodexpert.whatsdownloadplus.databinding.FragmentHomeBinding;

import java.io.File;
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
    private static final String DOWNLOAD_FOLDER_NAME = "Status Saver";
    private RecentDownloadsAdapter adapter;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(this);

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(HomeViewModel.class);

        setupClickListeners();
        observeViewModel();

        // Stats Update
        updateStreak();
        updateDownloadsStats(); // 🔥 Re-added for UI updates

        // App Version
        VersionHelper versionHelper = new VersionHelper(requireContext());
        binding.projectVersion.setText(versionHelper.getAppVersion());
        requireActivity().setTitle("Home");

        // RecyclerView Setup
        binding.rvRecentDownloads.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.rvRecentDownloads.setHasFixedSize(true);
        binding.rvRecentDownloads.setItemAnimator(null);

        handleIntentExtras();

        // Load & Set Adapter
        List<MediaItem> recentItems = getRecentMediaFromFolder();
        // NAYA (Ise use karen):
        adapter = new RecentDownloadsAdapter(recentItems, binding.tvRecentDownloadsEmpty);
        binding.rvRecentDownloads.setAdapter(adapter);
        return binding.getRoot();
    }

    private List<MediaItem> getRecentMediaFromFolder() {
        List<MediaItem> result = new ArrayList<>();
        Context context = getContext();
        if (context == null) return result;

        // 1. Direct Folder Path (Best for Android 9 & Instant Refresh)
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), DOWNLOAD_FOLDER_NAME);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null && files.length > 0) {
                // Latest files first (Sorting by last modified)
                java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

                for (File file : files) {
                    if (file.isFile()) {
                        String name = file.getName().toLowerCase();
                        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                                name.endsWith(".mp4") || name.endsWith(".mkv")) {

                            boolean isVideo = name.endsWith(".mp4") || name.endsWith(".mkv");

                            // 🔥 IMPORTANT: Android 9+ compatibility ke liye
                            // Hum yahan direct File Uri bhej rahe hain,
                            // Adapter isay FileProvider mein convert kar lega click par.
                            result.add(new MediaItem(Uri.fromFile(file), isVideo, file.lastModified()));
                        }
                    }
                    if (result.size() >= MAX_ITEMS) break;
                }
            }
        }

        // 2. Backup: MediaStore (Agar folder scan results na de, khas kar Android 11+ Scoped Storage mein)
        if (result.isEmpty()) {
            fetchMedia(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, result);
            fetchMedia(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, result);

            // Sorting MediaStore results by dateAdded
            Collections.sort(result, (a, b) -> Long.compare(b.dateAdded, a.dateAdded));
        }

        // Final limit check
        return result.size() > MAX_ITEMS ? new ArrayList<>(result.subList(0, MAX_ITEMS)) : result;
    }

    private void updateDownloadsStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Context context = getContext();
            if (context == null) return;

            // Today Midnight
            Calendar calToday = Calendar.getInstance();
            zeroTime(calToday);
            long todayTimestamp = calToday.getTimeInMillis() / 1000;

            // 7 Days Ago
            Calendar cal7Days = Calendar.getInstance();
            zeroTime(cal7Days);
            cal7Days.add(Calendar.DAY_OF_YEAR, -7);
            long sevenDaysTimestamp = cal7Days.getTimeInMillis() / 1000;

            int todayCount = getDownloadCountSince(context, todayTimestamp);
            int last7DaysCount = getDownloadCountSince(context, sevenDaysTimestamp);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.tvTodayCount.setText(String.valueOf(todayCount));
                        binding.tvLast7DaysCount.setText(String.valueOf(last7DaysCount));
                    }
                });
            }
        });
    }

    private int getDownloadCountSince(Context context, long sinceTimestamp) {
        int count = 0;
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), DOWNLOAD_FOLDER_NAME);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.lastModified() >= (sinceTimestamp * 1000L)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void fetchMedia(Context context, Uri uri, boolean isVideo, List<MediaItem> out) {
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
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ... handleIntentExtras, setupClickListeners, etc. (Baqi logic same hai jo aapne bheja) ...

    private void zeroTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private void setupClickListeners() {
        binding.cardImages.setOnClickListener(v -> openGallery(false));
        binding.cardVideos.setOnClickListener(v -> openGallery(true));
        binding.cardSaved.setOnClickListener(v -> navigateToDownload());
        binding.cardTodayDownloads.setOnClickListener(v -> animateCard((MaterialCardView) v, binding.tvTodayCount));
        binding.cardLast7Days.setOnClickListener(v -> animateCard((MaterialCardView) v, binding.tvLast7DaysCount));
        binding.cardActiveStreak.setOnClickListener(v -> animateCard((MaterialCardView) v, binding.tvActiveStreak));
    }

    private void animateCard(MaterialCardView card, TextView text) {
        CardLiquidAnimator.animate(card, text, 0xFFFFFF, 0x075E54, 600, 0.02f);
    }

    private void openGallery(boolean showVideos) {
        Bundle args = new Bundle();
        args.putBoolean("showVideos", showVideos);
        navController.navigate(R.id.nav_gallery, args);
    }

    private void navigateToDownload() {
        navController.navigate(R.id.nav_download);
    }

    private void observeViewModel() {
        viewModel.getJoinedDate().observe(getViewLifecycleOwner(), joinedText -> binding.joinedText.setText(joinedText));
        viewModel.getToolbarTitle().observe(getViewLifecycleOwner(), title -> {
            if (getActivity() != null) getActivity().setTitle(title);
        });
    }

    private void updateStreak() {
        Executors.newSingleThreadExecutor().execute(() -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("status_prefs", Context.MODE_PRIVATE);
            long lastOpenMillis = prefs.getLong("last_open_date", 0);
            int streak = prefs.getInt("active_streak", 0);
            long now = System.currentTimeMillis();

            Calendar today = Calendar.getInstance(); zeroTime(today);
            Calendar last = Calendar.getInstance(); last.setTimeInMillis(lastOpenMillis); zeroTime(last);

            int newStreak;
            if (lastOpenMillis == 0) newStreak = 1;
            else if (today.equals(last)) newStreak = streak;
            else {
                last.add(Calendar.DAY_OF_YEAR, 1);
                newStreak = today.equals(last) ? streak + 1 : 1;
            }
            prefs.edit().putInt("active_streak", newStreak).putLong("last_open_date", now).apply();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) binding.tvActiveStreak.setText(String.valueOf(newStreak));
                });
            }
        });
    }

    private void handleIntentExtras() {
        Intent intent = requireActivity().getIntent();
        if (intent != null && intent.hasExtra("openFragment")) {
            String fragmentToOpen = intent.getStringExtra("openFragment");
            if ("ImagesAndVideo".equals(fragmentToOpen)) {
                boolean isVideo = intent.getBooleanExtra("isVideo", false);
                Bundle args = new Bundle();
                args.putBoolean("showVideos", isVideo);
                navController.navigate(R.id.nav_gallery, args);
                intent.removeExtra("openFragment");
            }
        }
    }
    // 🔥 FIX: refresh logic moved here
    @Override
    public void onResume() {
        super.onResume();
        refreshAllData();
    }

    private void refreshAllData() {
        // 1. Stats and Streak
        updateStreak();
        updateDownloadsStats();

        // 2. Recent Downloads (Auto-refresh from folder)
        List<MediaItem> recentItems = getRecentMediaFromFolder();
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