package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(this);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupClickListeners();
        observeViewModel();

        // Stats Update
        updateStreak();
        updateDownloadsStats();

        // App Version
        VersionHelper versionHelper = new VersionHelper(requireContext());
        binding.projectVersion.setText(versionHelper.getAppVersion());
        requireActivity().setTitle("Home");

        // RecyclerView Setup
        binding.rvRecentDownloads.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvRecentDownloads.setHasFixedSize(true);

        handleIntentExtras();

        // Load Initial Media
        List<MediaItem> recentItems = getRecentMediaFromMediaStore();
        adapter = new RecentDownloadsAdapter(recentItems, binding.tvRecentDownloadsEmpty);
        binding.rvRecentDownloads.setAdapter(adapter);

        return binding.getRoot();
    }

    // 🔥 CLEANED: Android 10+ Optimized Media Fetching
    private List<MediaItem> getRecentMediaFromMediaStore() {
        List<MediaItem> result = new ArrayList<>();
        Context context = getContext();
        if (context == null) return result;

        // Dono Images aur Videos fetch karein
        fetchMedia(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, result);
        fetchMedia(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, result);

        // Sorting by Date (Newest First)
        Collections.sort(result, (a, b) -> Long.compare(b.dateAdded, a.dateAdded));

        // Limit to MAX_ITEMS
        if (result.size() > MAX_ITEMS) {
            return new ArrayList<>(result.subList(0, MAX_ITEMS));
        }
        return result;
    }

    private void fetchMedia(Context context, Uri uri, boolean isVideo, List<MediaItem> out) {
        // Sirf "Status Saver" folder ki files filter karein
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
                    long date = cursor.getLong(dateCol) * 1000L; // Seconds to Millis
                    out.add(new MediaItem(ContentUris.withAppendedId(uri, id), isVideo, date));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateDownloadsStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Context context = getContext();
            if (context == null) return;

            Calendar calToday = Calendar.getInstance(); zeroTime(calToday);
            long todayTimestamp = calToday.getTimeInMillis() / 1000;

            Calendar cal7Days = Calendar.getInstance(); zeroTime(cal7Days);
            cal7Days.add(Calendar.DAY_OF_YEAR, -7);
            long sevenDaysTimestamp = cal7Days.getTimeInMillis() / 1000;

            // Stats directly from MediaStore
            int todayCount = getCountFromMediaStore(context, todayTimestamp);
            int last7DaysCount = getCountFromMediaStore(context, sevenDaysTimestamp);

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

    private int getCountFromMediaStore(Context context, long sinceTimestamp) {
        int count = 0;
        String selection = MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ? AND " + MediaStore.MediaColumns.DATE_ADDED + " >= ?";
        String[] args = { "%" + DOWNLOAD_FOLDER_NAME + "%", String.valueOf(sinceTimestamp) };

        // Check Images
        count += queryCount(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, args);
        // Check Videos
        count += queryCount(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, selection, args);

        return count;
    }

    private int queryCount(Context context, Uri uri, String selection, String[] args) {
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns._ID}, selection, args, null)) {
            return (cursor != null) ? cursor.getCount() : 0;
        } catch (Exception e) { return 0; }
    }

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
        updateStreak();
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