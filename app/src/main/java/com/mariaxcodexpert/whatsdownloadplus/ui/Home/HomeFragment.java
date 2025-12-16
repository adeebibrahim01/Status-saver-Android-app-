package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.card.MaterialCardView;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.databinding.FragmentHomeBinding;

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

        setupClickListeners();
        observeViewModel();

        // Update streak on every app open
        updateStreak();

        // Update download stats using DownloadStatsManager
        updateDownloadsStats();

        requireActivity().setTitle("Home");
        return binding.getRoot();



    }

    private void updateStreak() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Context context = requireContext();
            final String PREFS_NAME = "status_prefs";
            final String KEY_LAST_OPEN = "last_open_date";
            final String KEY_STREAK = "active_streak";

            // Access SharedPreferences
            long lastOpenMillis = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getLong(KEY_LAST_OPEN, 0);
            int streak = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getInt(KEY_STREAK, 0);

            long today = System.currentTimeMillis();
            long oneDayMillis = 24 * 60 * 60 * 1000;
            int newStreak;

            if (lastOpenMillis == 0) {
                newStreak = 1; // first launch
            } else {
                long diff = today - lastOpenMillis;
                if (diff > 0 && diff <= oneDayMillis) {
                    newStreak = streak; // same day
                } else if (diff > oneDayMillis && diff <= 2 * oneDayMillis) {
                    newStreak = streak + 1; // consecutive day
                } else {
                    newStreak = 1; // missed day
                }
            }

            // Save updated streak and last open time
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_STREAK, newStreak)
                    .putLong(KEY_LAST_OPEN, today)
                    .apply();

            // Update UI on main thread
            requireActivity().runOnUiThread(() -> {
                if (binding != null && binding.tvActiveStreak != null) {
                    binding.tvActiveStreak.setText(String.valueOf(newStreak));
                }
            });
        });
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
            viewModel.setToolbarTitle("Download");
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
