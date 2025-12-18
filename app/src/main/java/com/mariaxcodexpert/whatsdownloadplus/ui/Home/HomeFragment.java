package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mariaxcodexpert.whatsdownloadplus.R;

import com.mariaxcodexpert.whatsdownloadplus.StatusWatcherWorker;
import com.mariaxcodexpert.whatsdownloadplus.VersionHelper;
import com.mariaxcodexpert.whatsdownloadplus.databinding.FragmentHomeBinding;
import com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo.SavedFilesDB;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import android.view.View;
import android.widget.Toast;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private NavController navController;
    private HomeViewModel viewModel;

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

        // Update streak & downloads
        updateStreak();
        updateDownloadsStats();

        // Set app version
        VersionHelper versionHelper = new VersionHelper(requireContext());
        String version = versionHelper.getAppVersion();
        binding.projectVersion.setText(version);
        requireActivity().setTitle("Home");

        RecyclerView rvRecentDownloads = binding.rvRecentDownloads;
        rvRecentDownloads.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

// Load recent media from Status Saver folder
        List<MediaItem> recentItems = getRecentMediaFromFolder();
        RecentDownloadsAdapter adapter = new RecentDownloadsAdapter(getContext(), recentItems);
        rvRecentDownloads.setAdapter(adapter);
        return binding.getRoot();
    }
    private List<MediaItem> getRecentMediaFromFolder() {
        List<MediaItem> list = new ArrayList<>();
        Context context = getContext();
        if (context == null) {
            Toast.makeText(getContext(), "Context is null", Toast.LENGTH_SHORT).show();
            return list;
        }

        String folderName = "Status Saver"; // Correct folder name

        // --- Images ---
        Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] imageProjection = { MediaStore.Images.Media._ID, MediaStore.Images.Media.RELATIVE_PATH };
        String imageSelection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        String[] imageSelectionArgs = { "%" + folderName + "%" };
        String imageSortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = context.getContentResolver().query(
                imagesUri, imageProjection, imageSelection, imageSelectionArgs, imageSortOrder
        )) {
            if (cursor == null) {
                Toast.makeText(context, "Image cursor is null", Toast.LENGTH_SHORT).show();
            } else if (cursor.getCount() == 0) {
                Toast.makeText(context, "No images found in folder: " + folderName, Toast.LENGTH_SHORT).show();
            } else {
                int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idIndex);
                    Uri contentUri = Uri.withAppendedPath(imagesUri, String.valueOf(id));
                    list.add(new MediaItem(contentUri, false));
                }
                Toast.makeText(context, "Images found: " + list.size(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error loading images: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // --- Videos ---
        Uri videosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] videoProjection = { MediaStore.Video.Media._ID, MediaStore.Video.Media.RELATIVE_PATH };
        String videoSelection = MediaStore.Video.Media.RELATIVE_PATH + " LIKE ?";
        String[] videoSelectionArgs = { "%" + folderName + "%" };
        String videoSortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = context.getContentResolver().query(
                videosUri, videoProjection, videoSelection, videoSelectionArgs, videoSortOrder
        )) {
            if (cursor == null) {
                Toast.makeText(context, "Video cursor is null", Toast.LENGTH_SHORT).show();
            } else if (cursor.getCount() == 0) {
                Toast.makeText(context, "No videos found in folder: " + folderName, Toast.LENGTH_SHORT).show();
            } else {
                int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idIndex);
                    Uri contentUri = Uri.withAppendedPath(videosUri, String.valueOf(id));
                    list.add(new MediaItem(contentUri, true));
                }
                Toast.makeText(context, "Videos found: " + list.size(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error loading videos: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        if (list.isEmpty()) {
            Toast.makeText(context, "Recent Downloads folder is empty", Toast.LENGTH_SHORT).show();
        }

        return list;
    }




    private void updateStreak() {
        Executors.newSingleThreadExecutor().execute(() -> {

            Context context = requireContext();
            final String PREFS_NAME = "status_prefs";
            final String KEY_LAST_OPEN = "last_open_date";
            final String KEY_STREAK = "active_streak";

            SharedPreferences prefs =
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            long lastOpenMillis = prefs.getLong(KEY_LAST_OPEN, 0);
            int streak = prefs.getInt(KEY_STREAK, 0);

            long now = System.currentTimeMillis();
            int newStreak;

            Calendar todayCal = Calendar.getInstance();
            todayCal.setTimeInMillis(now);
            zeroTime(todayCal);

            Calendar lastCal = Calendar.getInstance();
            lastCal.setTimeInMillis(lastOpenMillis);
            zeroTime(lastCal);

            if (lastOpenMillis == 0) {
                // First launch ever
                newStreak = 1;
            } else if (todayCal.equals(lastCal)) {
                // Same day → no change
                newStreak = streak;
            } else {
                lastCal.add(Calendar.DAY_OF_YEAR, 1);

                if (todayCal.equals(lastCal)) {
                    // Consecutive day
                    newStreak = streak + 1;
                } else {
                    // Missed day
                    newStreak = 1;
                }
            }

            prefs.edit()
                    .putInt(KEY_STREAK, newStreak)
                    .putLong(KEY_LAST_OPEN, now)
                    .apply();

            requireActivity().runOnUiThread(() -> {
                if (binding != null && binding.tvActiveStreak != null) {
                    binding.tvActiveStreak.setText(String.valueOf(newStreak));
                }
            });
        });
    }

    private void zeroTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }


    private void updateDownloadsStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Context context = requireContext();

            // ✅ Use existing SavedFilesDB instance
            SavedFilesDB savedFilesDB = new SavedFilesDB(context);

            // Pass savedFilesDB to DownloadStatsManager
            DownloadStatsManager statsManager = new DownloadStatsManager(context, savedFilesDB);

            // Update UI on main thread
            requireActivity().runOnUiThread(() -> {
                if (binding != null) {
                    binding.tvTodayCount.setText(String.valueOf(savedFilesDB.getTodayCount()));;
                    binding.tvLast7DaysCount.setText(String.valueOf(savedFilesDB.getLast7DaysCount()));
                }
            });
        });
    }


    private void observeViewModel() {
        viewModel.getJoinedDate().observe(getViewLifecycleOwner(), joinedText -> {
            binding.joinedText.setText(joinedText);
        });

        viewModel.getToolbarTitle().observe(getViewLifecycleOwner(), title -> {
            requireActivity().setTitle(title);
        });
    }

    private void setupClickListeners() {
        binding.cardImages.setOnClickListener(v -> openGallery(false));  // false = Images
        binding.cardVideos.setOnClickListener(v -> openGallery(true));  // true = Videos
        binding.cardSaved.setOnClickListener(v -> navigateToDownload());
        // --- Today Card Animation ---

// Today Downloads Card → Green
        // Today Downloads Card → WhatsApp Green Gradient
        binding.cardTodayDownloads.setOnClickListener(v ->
                CardLiquidAnimator.animate(
                        binding.cardTodayDownloads,
                        binding.tvTodayCount,
                        0xFFFFFF,
                        0x075E54,
                        600,       // duration 2 seconds
                        0.02f       // pulse scale
                )
        );

// Last 7 Days Card → WhatsApp Light Green / Teal Gradient
        binding.cardLast7Days.setOnClickListener(v ->
                CardLiquidAnimator.animate(
                        binding.cardLast7Days,
                        binding.tvLast7DaysCount,
                        0xFFFFFF,
                        0x075E54,
                        600,
                        0.02f
                )
        );

// Active Streak Card → WhatsApp Teal / Cyan Gradient
        binding.cardActiveStreak.setOnClickListener(v ->
                CardLiquidAnimator.animate(
                        binding.cardActiveStreak,
                        binding.tvActiveStreak,
                        0xFFFFFF,
                        0x075E54,
                        600,
                        0.02f
                )
        );

    }

    private void openGallery(boolean showVideos) {
        Bundle args = new Bundle();
        args.putBoolean("showVideos", showVideos);

        try {
            navController.navigate(R.id.nav_gallery, args);
            viewModel.setToolbarTitle(showVideos ? "Videos" : "Images");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToDownload() {
        try {
            navController.navigate(R.id.nav_download);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
