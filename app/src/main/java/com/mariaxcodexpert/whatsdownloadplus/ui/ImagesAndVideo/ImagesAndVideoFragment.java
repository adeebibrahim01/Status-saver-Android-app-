package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.google.android.material.tabs.TabLayout;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImagesAndVideoFragment extends Fragment {

    private TabLayout tabLayout;
    private ImagesAndVideoAdapter imageAdapter, videoAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ConstraintLayout emptyStateLayout;
    private LottieAnimationView lottieEmptyState;
    private TextView tvEmptyMessage;
    private ProgressBar progressBar;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Uri statusFolderUri;
    private ImagesAndVideoViewModel viewModel;
    private ExecutorService executor;
    private RequestManager glide;
    private RecyclerView recyclerImages, recyclerVideos;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_imagesandvideo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initial Setup & Resource Management
        executor = Executors.newFixedThreadPool(2);
        viewModel = new ViewModelProvider(requireActivity()).get(ImagesAndVideoViewModel.class);
        glide = Glide.with(this);

        initViews(view);

        // 2. 🔥 Notification/Intent Argument Check
        // "showVideos" true hoga toh direct Video Tab par focus jayega
        boolean isVideoDirect = getArguments() != null && getArguments().getBoolean("showVideos", false);

        // 3. 🔥 Setup Tabs & Initial Selection
        if (tabLayout.getTabCount() == 0) {
            tabLayout.addTab(tabLayout.newTab().setText("Images"), !isVideoDirect);
            tabLayout.addTab(tabLayout.newTab().setText("Videos"), isVideoDirect);
        }

        // 4. 🔥 Force Visibility based on Arguments
        if (isVideoDirect) {
            recyclerImages.setVisibility(View.GONE);
            recyclerVideos.setVisibility(View.VISIBLE);

            // Auto-select the Video Tab (Index 1) with a tiny delay for UI stability
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && tabLayout.getTabCount() > 1) {
                    TabLayout.Tab videoTab = tabLayout.getTabAt(1);
                    if (videoTab != null && !videoTab.isSelected()) {
                        videoTab.select();
                    }
                }
            }, 100);
        } else {
            recyclerImages.setVisibility(View.VISIBLE);
            recyclerVideos.setVisibility(View.GONE);
        }

        // 5. Core Operations
        initAdapters();
        loadStatusFolderUri();
        setupTabs(); // Ensure this listener doesn't conflict with initial selection
        setupSwipeRefresh();
        observeViewModel();

        // 6. Lottie & UI Optimization
        if (lottieEmptyState != null) {
            lottieEmptyState.setCacheComposition(true);
        }

        // 7. Data Load Execution
        loadStatuses();
    }
    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerImages = view.findViewById(R.id.recyclerImages);
        recyclerVideos = view.findViewById(R.id.recyclerVideos);

        // Optimization: Shared View Pool for memory efficiency
        RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();

        setupRecyclerView(recyclerImages, sharedPool);
        setupRecyclerView(recyclerVideos, sharedPool);

        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        lottieEmptyState = view.findViewById(R.id.lottieEmptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        progressBar = view.findViewById(R.id.progressBarLoading);

        recyclerImages.setHasFixedSize(true);
        recyclerVideos.setHasFixedSize(true);
// Views ko pehle se inflate kar ke rakhne ke liye:
        recyclerImages.setItemViewCacheSize(20);
        recyclerVideos.setItemViewCacheSize(20);
    }

    private void setupRecyclerView(RecyclerView rv, RecyclerView.RecycledViewPool pool) {
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20); // Smooth scrolling optimization
        rv.setRecycledViewPool(pool);

        // 🔥 BLINK FIX: Ye lines yahan add karein
        if (rv.getItemAnimator() != null) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) rv.getItemAnimator())
                    .setSupportsChangeAnimations(false);
        }
    }

    private void initAdapters() {
        imageAdapter = new ImagesAndVideoAdapter(requireContext(), glide, false);
        videoAdapter = new ImagesAndVideoAdapter(requireContext(), glide, true);

        recyclerImages.setAdapter(imageAdapter);
        recyclerVideos.setAdapter(videoAdapter);

        // Custom countdown attach
        imageAdapter.startCountdownUpdater(recyclerImages);
        videoAdapter.startCountdownUpdater(recyclerVideos);
    }

    private void observeViewModel() {
        viewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            imageAdapter.submitList(images);
            updateEmptyState();
        });

        viewModel.getVideos().observe(getViewLifecycleOwner(), videos -> {
            videoAdapter.submitList(videos);
            updateEmptyState();
        });
    }

    private void setupTabs() {
        // Listener sirf user ke manual click ko handle karne ke liye hona chahiye
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Sirf tab change hone par visibility badlein
                boolean isVideo = (tab.getPosition() == 1);
                recyclerImages.setVisibility(isVideo ? View.GONE : View.VISIBLE);
                recyclerVideos.setVisibility(isVideo ? View.VISIBLE : View.GONE);
                updateEmptyState();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void switchTabContent(boolean isVideo) {
        recyclerImages.setVisibility(isVideo ? View.GONE : View.VISIBLE);
        recyclerVideos.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        updateEmptyState();
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadStatuses);
    }

    private void loadStatusFolderUri() {
        String uri = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("statusFolderUri", null);
        statusFolderUri = uri != null ? Uri.parse(uri) : null;
    }

    private void loadStatuses() {
        if (statusFolderUri == null || executor == null || executor.isShutdown()) {
            swipeRefreshLayout.setRefreshing(false);
            progressBar.setVisibility(View.GONE);
            updateEmptyState();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            DocumentFile folder = DocumentFile.fromTreeUri(requireContext(), statusFolderUri);
            List<DocumentFile> images = new ArrayList<>();
            List<DocumentFile> videos = new ArrayList<>();

            if (folder != null && folder.isDirectory()) {
                DocumentFile[] files = folder.listFiles();
                for (DocumentFile file : files) {
                    if (file != null && file.isFile()) {
                        String n = file.getName();
                        if (n == null) continue;
                        String nameLower = n.toLowerCase();
                        if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg") || nameLower.endsWith(".png") || nameLower.endsWith(".webp")) {
                            images.add(file);
                        } else if (nameLower.endsWith(".mp4") || nameLower.endsWith(".mkv") || nameLower.endsWith(".3gp")) {
                            videos.add(file);
                        }
                    }
                }
            }

            handler.post(() -> {
                if (!isAdded()) return;

                // ViewModel update karein
                viewModel.setImages(images);
                viewModel.setVideos(videos);

                // Blink Fix: Delay thoda barha dein taake Glide cache se images utha le
                handler.postDelayed(() -> {
                    if (isAdded()) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        updateEmptyState();
                    }
                }, 300); // 300ms is safer for smooth transition
            });
        });
    }

    private void updateEmptyState() {
        if (!isAdded()) return;

        int selectedTab = tabLayout.getSelectedTabPosition();
        boolean isVideoTab = (selectedTab == 1);

        List<DocumentFile> images = viewModel.getImages().getValue();
        List<DocumentFile> videos = viewModel.getVideos().getValue();
        List<DocumentFile> currentList = isVideoTab ? videos : images;

        boolean isCurrentListEmpty = (currentList == null || currentList.isEmpty());

        if (isCurrentListEmpty) {
            recyclerImages.setVisibility(View.GONE);
            recyclerVideos.setVisibility(View.GONE);

            // Text Fix: Force visibility on sub-views
            emptyStateLayout.setVisibility(View.VISIBLE);
            tvEmptyMessage.setVisibility(View.VISIBLE);
            lottieEmptyState.setVisibility(View.VISIBLE);

            String msg = isVideoTab ?
                    "No Videos Found!\nWatch status on WhatsApp first." :
                    "No Images Found!\nWatch status on WhatsApp first.";
            tvEmptyMessage.setText(msg);

            if (!lottieEmptyState.isAnimating()) {
                lottieEmptyState.setAnimation(R.raw.empty_status);
                lottieEmptyState.playAnimation();
            }
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            // Recycler visibility switch
            if (isVideoTab) {
                recyclerVideos.setVisibility(View.VISIBLE);
                recyclerImages.setVisibility(View.GONE);
            } else {
                recyclerImages.setVisibility(View.VISIBLE);
                recyclerVideos.setVisibility(View.GONE);
            }
        }
    }
    @Override
    public void onDestroyView() {
        // Shutdown logic optimized to prevent memory leaks
        if (imageAdapter != null) imageAdapter.shutdown();
        if (videoAdapter != null) videoAdapter.shutdown();
        if (executor != null) executor.shutdownNow();
        super.onDestroyView();
    }
}