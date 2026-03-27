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
import androidx.recyclerview.widget.RecyclerView;

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
    // Folder path constant for consistency
    private static final String DOWNLOAD_FOLDER_PATH = Environment.DIRECTORY_PICTURES + "/Status Saver";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(HomeViewModel.class);

        setupClickListeners();
        observeViewModel();

        // Update streak & downloads stats from storage
        updateStreak();
        updateDownloadsStats();

        // Set app version
        VersionHelper versionHelper = new VersionHelper(requireContext());
        binding.projectVersion.setText(versionHelper.getAppVersion());
        requireActivity().setTitle("Home");

        // Setup Recent Downloads RecyclerView
        binding.rvRecentDownloads.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        handleIntentExtras();

        // Load recent media from Status Saver folder
        List<MediaItem> recentItems = getRecentMediaFromFolder();
        // 1. Naya constructor ab sirf items aur emptyMessage mangta hai (Context nahi)
        RecentDownloadsAdapter adapter = new RecentDownloadsAdapter(recentItems, binding.tvRecentDownloadsEmpty);

// 2. Adapter set karein
        binding.rvRecentDownloads.setAdapter(adapter);

// 3. RecyclerView Optimization (Smooth scrolling ke liye)
        binding.rvRecentDownloads.setHasFixedSize(true);
// ItemAnimator ko null karne se DiffUtil ki wajah se hone wali halki "blink" khatam ho jati hai
        binding.rvRecentDownloads.setItemAnimator(null);

        return binding.getRoot();
    }

    private void handleIntentExtras() {
        Intent intent = requireActivity().getIntent();
        if (intent != null && intent.hasExtra("openFragment")) {
            String fragmentToOpen = intent.getStringExtra("openFragment");
            if ("ImagesAndVideo".equals(fragmentToOpen)) {
                boolean isVideo;
                if (intent.hasExtra("isVideo")) {
                    isVideo = intent.getBooleanExtra("isVideo", false);
                } else {
                    int statusId = intent.getIntExtra("statusId", -1);
                    isVideo = statusId != -1 && statusIdIsVideo(statusId);
                }
                Bundle args = new Bundle();
                args.putBoolean("showVideos", isVideo);
                navController.navigate(R.id.nav_gallery, args);
            }
        }
    }

    private boolean statusIdIsVideo(int statusId) {
        File statusFolder = new File(requireContext().getExternalFilesDir(null), "../../WhatsApp/Media/.Statuses");
        if (statusFolder.exists() && statusFolder.isDirectory()) {
            File[] files = statusFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().hashCode() == statusId) {
                        String name = file.getName().toLowerCase();
                        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi");
                    }
                }
            }
        }
        return false;
    }

    private List<MediaItem> getRecentMediaFromFolder() {
        Context context = getContext();
        if (context == null) return Collections.emptyList();
        List<MediaItem> result = new ArrayList<>();

        fetchMedia(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, result);
        fetchMedia(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, result);

        Collections.sort(result, (a, b) -> Long.compare(b.dateAdded, a.dateAdded));
        return result.size() > MAX_ITEMS ? result.subList(0, MAX_ITEMS) : result;
    }

    private void fetchMedia(Context context, Uri uri, boolean isVideo, List<MediaItem> out) {
        String selection = MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?";
        String[] args = { "%" + DOWNLOAD_FOLDER_PATH + "%" };
        String sortOrder = MediaStore.MediaColumns.DATE_ADDED + " DESC";

        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED},
                selection, args, sortOrder)) {
            if (cursor == null) return;
            int idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);

            while (cursor.moveToNext() && out.size() < MAX_ITEMS * 2) {
                long id = cursor.getLong(idCol);
                long date = cursor.getLong(dateCol) * 1000L;
                out.add(new MediaItem(ContentUris.withAppendedId(uri, id), isVideo, date));
            }
        } catch (Exception e) { e.printStackTrace(); }
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
            requireActivity().runOnUiThread(() -> {
                if (binding != null) binding.tvActiveStreak.setText(String.valueOf(newStreak));
            });
        });
    }

    private void updateDownloadsStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Context context = getContext();
            if (context == null) return;

            // Today Midnight
            Calendar calToday = Calendar.getInstance();
            zeroTime(calToday);
            long todayTimestamp = calToday.getTimeInMillis() / 1000;

            // 7 Days Ago Midnight
            Calendar cal7Days = Calendar.getInstance();
            zeroTime(cal7Days);
            cal7Days.add(Calendar.DAY_OF_YEAR, -7);
            long sevenDaysTimestamp = cal7Days.getTimeInMillis() / 1000;

            int todayCount = getDownloadCountSince(context, todayTimestamp);
            int last7DaysCount = getDownloadCountSince(context, sevenDaysTimestamp);

            requireActivity().runOnUiThread(() -> {
                if (binding != null) {
                    binding.tvTodayCount.setText(String.valueOf(todayCount));
                    binding.tvLast7DaysCount.setText(String.valueOf(last7DaysCount));
                }
            });
        });
    }

    private int getDownloadCountSince(Context context, long sinceTimestamp) {
        int count = 0;
        Uri[] uris = {MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI};
        String selection = MediaStore.MediaColumns.DATE_ADDED + " >= ? AND " + MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?";
        String[] args = { String.valueOf(sinceTimestamp), "%" + DOWNLOAD_FOLDER_PATH + "%" };

        for (Uri uri : uris) {
            try (Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns._ID}, selection, args, null)) {
                if (cursor != null) count += cursor.getCount();
            } catch (Exception e) { e.printStackTrace(); }
        }
        return count;
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

        binding.cardTodayDownloads.setOnClickListener(v ->
                animateCard((MaterialCardView) v, binding.tvTodayCount)
        );

        binding.cardLast7Days.setOnClickListener(v -> animateCard((MaterialCardView) v, binding.tvLast7DaysCount));
        binding.cardActiveStreak.setOnClickListener(v -> animateCard((MaterialCardView) v, binding.tvActiveStreak));
    }

    private void animateCard(MaterialCardView card, TextView text) {
        CardLiquidAnimator.animate(card, text, 0xFFFFFF, 0x075E54, 600, 0.02f);
    }

    private void openGallery(boolean showVideos) {
        Bundle args = new Bundle();
        args.putBoolean("showVideos", showVideos); // Ye signal hai Fragment ke liye
        try {
            // NavOptions add kar sakte hain smooth transition ke liye (Optional)
            navController.navigate(R.id.nav_gallery, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void navigateToDownload() {
        try { navController.navigate(R.id.nav_download); } catch (Exception e) { e.printStackTrace(); }
    }

    private void observeViewModel() {
        viewModel.getJoinedDate().observe(getViewLifecycleOwner(), joinedText -> binding.joinedText.setText(joinedText));
        viewModel.getToolbarTitle().observe(getViewLifecycleOwner(), title -> requireActivity().setTitle(title));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}