package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.mariaxcodexpert.whatsdownloadplus.R;

import com.mariaxcodexpert.whatsdownloadplus.StatusWatcherWorker;
import com.mariaxcodexpert.whatsdownloadplus.VersionHelper;
import com.mariaxcodexpert.whatsdownloadplus.databinding.FragmentHomeBinding;


import java.util.Calendar;
import java.util.concurrent.Executors;

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
        // Schedule background status watcher
//        StatusWatcherWorker.scheduleWork(requireContext());

        setupClickListeners();
        observeViewModel();

        // Update streak on every app open
        updateStreak();

        // Update download stats using DownloadStatsManager
        updateDownloadsStats();
        // Inside onCreateView() of HomeFragment
        VersionHelper versionHelper = new VersionHelper(requireContext()); // good
        String version = versionHelper.getAppVersion();
        binding.projectVersion.setText(version);
        requireActivity().setTitle("Home");
        return binding.getRoot();


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

            // Use DownloadStatsManager to get today and last 7 days downloads
            DownloadStatsManager statsManager = new DownloadStatsManager(context);
            int todayCount = statsManager.getTodayDownloads();        // today downloads
            int last7DaysCount = statsManager.getLast7DaysDownloads(); // last 7 days total

            // Update UI on main thread
            requireActivity().runOnUiThread(() -> {
                if (binding != null) {
                    binding.tvTodayCount.setText(String.valueOf(todayCount));
                    binding.tvLast7DaysCount.setText(String.valueOf(last7DaysCount)); // show last 7 days
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
