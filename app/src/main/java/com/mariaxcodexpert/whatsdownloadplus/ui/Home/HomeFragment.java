package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.util.Log;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.*;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.StatsActivity;
import com.mariaxcodexpert.whatsdownloadplus.databinding.FragmentHomeBinding;
import com.mariaxcodexpert.whatsdownloadplus.ui.Download.FullScreenMediaActivity;
import java.util.*;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private RecentDownloadsAdapter adapter;
    private boolean isNavigating = false;
    private final Handler navHandler = new Handler(Looper.getMainLooper());
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle saved) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // 🔥 IS LINE KO AISE LIKHEIN (Bina // ke)
        //throw new RuntimeException("Test Crash - Status Saver App");

         return binding.getRoot(); // Ye line execute nahi hogi kyunki upar crash ho jayega
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        setupUI();
        observeRoomData();
    }

    private void setupUI() {
        binding.rvRecentDownloads.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        adapter = new RecentDownloadsAdapter(item -> {
            if (isNavigating || !isAdded()) return;
            isNavigating = true;

            List<Object> currentList = adapter.getCurrentList();
            ArrayList<java.io.Serializable> serializableList = new ArrayList<>();
            for (Object obj : currentList) {
                if (obj instanceof java.io.Serializable) {
                    serializableList.add((java.io.Serializable) obj);
                }
            }
            int position = currentList.indexOf(item);

            AdManager.showInterstitial(getActivity(), new AdManager.AdCallback() {
                @Override
                public void onAdClosed() {
                    openMedia(serializableList, position);
                }

                @Override
                public void onAdFailed() {
                    openMedia(serializableList, position);
                }
            });

            navHandler.postDelayed(() -> isNavigating = false, 1000);
        });
//        binding.tvTotalStats.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), StatsActivity.class);
//            startActivity(intent);
//        });

        binding.rvRecentDownloads.setAdapter(adapter);

        if (binding.rvRecentDownloads.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) Objects.requireNonNull(binding.rvRecentDownloads.getItemAnimator()))
                    .setSupportsChangeAnimations(false);
        }
        // 🔥 Dashboard Icons Animation
        Animation floatingAnim = AnimationUtils.loadAnimation(getContext(), R.anim.dashboard_icon_anim);
        binding.ivImagesIcon.startAnimation(floatingAnim);

      //  Animation floatingAnimunique = AnimationUtils.loadAnimation(getContext(), R.anim.dashboard_icon_unique_anim);
        binding.ivdashboardIcon.startAnimation(floatingAnim);
// Example: "Premium Discovery" text par animation
   //     proTypewriter(binding.headerTitle, "Status Saver");

        // Apne ImageView ki ID check karke yahan likhen
        binding.btnImages.setOnClickListener(v -> safeAction(v, () -> navigateToGallery(false)));
        binding.btnVideos.setOnClickListener(v -> safeAction(v, () -> navigateToGallery(true)));
        binding.btnSaved.setOnClickListener(v -> safeAction(v, () -> {
            try {
                NavHostFragment.findNavController(this).navigate(R.id.nav_download);
            } catch (Exception e) { e.printStackTrace(); }
        }));

        try {
            binding.projectVersion.setText(com.mariaxcodexpert.whatsdownloadplus.VersionHelper.getAppVersion(requireContext()));
        } catch (Exception e) {
            binding.projectVersion.setText("v1.0.0");
        }
    }
    private void proTypewriter(TextView tv, String fullText) {
        Handler handler = new Handler();
        final int[] index = {0};

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index[0] <= fullText.length()) {
                    // Har character k sath cursor add kerna
                    String displayedText = fullText.substring(0, index[0]) + (index[0] % 2 == 0 ? "|" : " ");
                    tv.setText(displayedText);
                    index[0]++;
                    handler.postDelayed(this, 120); // Speed control
                } else {
                    // Typing khatam hone k baad cursor hata dena
                    tv.setText(fullText);
                }
            }
        }, 120);
    }

    private void openMedia(ArrayList<java.io.Serializable> list, int position) {
        if (getContext() == null) return;
        Intent intent = new Intent(getContext(), FullScreenMediaActivity.class);
        intent.putExtra(com.mariaxcodexpert.whatsdownloadplus.ui.Download.MediaListAdapter.EXTRA_MEDIA_LIST, list);
        intent.putExtra(com.mariaxcodexpert.whatsdownloadplus.ui.Download.MediaListAdapter.EXTRA_POSITION, position);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void observeRoomData() {
        viewModel.dashboardStats.observe(getViewLifecycleOwner(), s -> {
            if (s == null || binding == null) return;

            // 🔴 Real-time Log for debugging
            Log.d("HOME_UI_UPDATE", "Today: " + s.todayCount + " | Saved: " + s.totalCount);

            // Update with basic animation
            updateAnimText(binding.tvTodayCount, String.valueOf(s.todayCount));
            updateAnimText(binding.tvLast7DaysCount, String.valueOf(s.totalCount));
            updateAnimText(binding.tvActiveStreak, String.valueOf(s.activeStatuses));

            binding.joinedText.setText(s.joinedDate);

            // Progress bars hide and Text show
            binding.pbTodayCount.setVisibility(View.GONE);
            binding.pbLast7Days.setVisibility(View.GONE);
            binding.pbActiveStreak.setVisibility(View.GONE);
            binding.tvTodayCount.setVisibility(View.VISIBLE);
            binding.tvLast7DaysCount.setVisibility(View.VISIBLE);
            binding.tvActiveStreak.setVisibility(View.VISIBLE);
        });

        viewModel.recentImages.observe(getViewLifecycleOwner(), i -> updateList());
        viewModel.recentVideos.observe(getViewLifecycleOwner(), v -> updateList());
    }

    private void updateList() {
        if (binding == null) return;

        List<Object> combined = new ArrayList<>();

        // LiveData se current values uthana
        List<com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity> images = viewModel.recentImages.getValue();
        List<com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity> videos = viewModel.recentVideos.getValue();

        if (images != null) combined.addAll(images);
        if (videos != null) combined.addAll(videos);

        // 🔥 LATEST DOWNLOADED FIRST: Sorting by lastModified descending
        Collections.sort(combined, (o1, o2) -> {
            long t1 = (o1 instanceof com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity) ?
                    ((com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity) o1).lastModified :
                    ((com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity) o1).lastModified;
            long t2 = (o2 instanceof com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity) ?
                    ((com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity) o2).lastModified :
                    ((com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity) o2).lastModified;
            return Long.compare(t2, t1);
        });

        // UI visibility handle karna
        boolean empty = combined.isEmpty();
        binding.rvRecentDownloads.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.tvRecentDownloadsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);

        // ListAdapter ko data pass karna (New instance bhejni chahiye hamesha)
        if (!empty) {
            adapter.submitList(new ArrayList<>(combined));
        } else {
            adapter.submitList(null);
        }
    }

    private void updateAnimText(TextView tv, String text) {
        if (text == null || tv == null) return;
        if (tv.getText().toString().equals(text)) return;

        AlphaAnimation fade = new AlphaAnimation(0.5f, 1.0f);
        fade.setDuration(300);
        tv.startAnimation(fade);
        tv.setText(text);
    }

    private void safeAction(View v, Runnable action) {
        if (isNavigating) return;
        isNavigating = true;

        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(120)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .withEndAction(action)
                            .start();
                })
                .start();

        navHandler.postDelayed(() -> isNavigating = false, 800);
    }

    private void navigateToGallery(boolean isVideo) {
        Bundle b = new Bundle();
        b.putBoolean("arg_is_video", isVideo);
        try {
            NavHostFragment.findNavController(this).navigate(R.id.nav_gallery, b);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isNavigating = false;
        if (viewModel != null) {
            viewModel.resetSyncPhase();
            // 🔥 Refresh data to show updated counts immediately after deletion/download
            viewModel.refreshDashboardData();
        }
    }

    @Override public void onDestroyView() {
        navHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
        binding = null;
    }
}