package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.Arrays;
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

    // --- NEW: Launcher to handle back from Preview Activity ---
    private final ActivityResultLauncher<Intent> previewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Refresh both adapters to update download/saved icons
                    if (imageAdapter != null) imageAdapter.notifyDataSetChanged();
                    if (videoAdapter != null) videoAdapter.notifyDataSetChanged();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_imagesandvideo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        executor = Executors.newFixedThreadPool(2);
        viewModel = new ViewModelProvider(requireActivity()).get(ImagesAndVideoViewModel.class);
        glide = Glide.with(this);

        initViews(view);

        boolean isVideoDirect = getArguments() != null && getArguments().getBoolean("showVideos", false);

        if (tabLayout.getTabCount() == 0) {
            tabLayout.addTab(tabLayout.newTab().setText("Images"), !isVideoDirect);
            tabLayout.addTab(tabLayout.newTab().setText("Videos"), isVideoDirect);
        }

        if (isVideoDirect) {
            recyclerImages.setVisibility(View.GONE);
            recyclerVideos.setVisibility(View.VISIBLE);

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

        initAdapters();
        loadStatusFolderUri();
        setupTabs();
        setupSwipeRefresh();
        observeViewModel();

        if (lottieEmptyState != null) {
            lottieEmptyState.setCacheComposition(true);
        }

        loadStatuses();
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerImages = view.findViewById(R.id.recyclerImages);
        recyclerVideos = view.findViewById(R.id.recyclerVideos);

        RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();

        setupRecyclerView(recyclerImages, sharedPool);
        setupRecyclerView(recyclerVideos, sharedPool);

        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        lottieEmptyState = view.findViewById(R.id.lottieEmptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        progressBar = view.findViewById(R.id.progressBarLoading);

        recyclerImages.setHasFixedSize(true);
        recyclerVideos.setHasFixedSize(true);
        recyclerImages.setItemViewCacheSize(20);
        recyclerVideos.setItemViewCacheSize(20);
    }

    private void setupRecyclerView(RecyclerView rv, RecyclerView.RecycledViewPool pool) {
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20);
        rv.setRecycledViewPool(pool);

        if (rv.getItemAnimator() != null) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) rv.getItemAnimator())
                    .setSupportsChangeAnimations(false);
        }
    }

    private void initAdapters() {
        // --- Updated with OnItemClickListener ---
        imageAdapter = new ImagesAndVideoAdapter(requireContext(), glide, false, (file, isVideo) -> {
            openPreviewWithLauncher(file, isVideo);
        });

        videoAdapter = new ImagesAndVideoAdapter(requireContext(), glide, true, (file, isVideo) -> {
            openPreviewWithLauncher(file, isVideo);
        });

        recyclerImages.setAdapter(imageAdapter);
        recyclerVideos.setAdapter(videoAdapter);

        imageAdapter.startCountdownUpdater(recyclerImages);
        videoAdapter.startCountdownUpdater(recyclerVideos);
    }

    // Helper function to launch Preview Activity via Launcher
    private void openPreviewWithLauncher(DocumentFile file, boolean isVideo) {
        Intent intent = new Intent(requireContext(), ImageVideoPreviewActivity.class);
        intent.putExtra("uri", file.getUri().toString());
        intent.putExtra("is_video", isVideo);
        previewLauncher.launch(intent);
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
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean isVideo = (tab.getPosition() == 1);
                recyclerImages.setVisibility(isVideo ? View.GONE : View.VISIBLE);
                recyclerVideos.setVisibility(isVideo ? View.VISIBLE : View.GONE);
                updateEmptyState();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            recyclerImages.setVisibility(View.GONE);
            recyclerVideos.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
            loadStatuses();
        });
    }

    private void loadStatusFolderUri() {
        String uri = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("statusFolderUri", null);
        statusFolderUri = uri != null ? Uri.parse(uri) : null;
    }

    private void loadStatuses() {
        if (getContext() == null || statusFolderUri == null || executor == null || executor.isShutdown()) {
            stopLoadingUI();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerImages.setVisibility(View.GONE);
        recyclerVideos.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        executor.execute(() -> {
            DocumentFile folder = DocumentFile.fromTreeUri(requireContext(), statusFolderUri);
            List<DocumentFile> imageList = new ArrayList<>();
            List<DocumentFile> videoList = new ArrayList<>();

            if (folder != null && folder.exists() && folder.isDirectory()) {
                DocumentFile[] allFiles = folder.listFiles();
                if (allFiles != null) {
                    Arrays.sort(allFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                    for (DocumentFile file : allFiles) {
                        if (file.isFile() && file.getName() != null) {
                            String name = file.getName().toLowerCase();
                            if (isImage(name)) {
                                imageList.add(file);
                            } else if (isVideo(name)) {
                                videoList.add(file);
                            }
                        }
                    }
                }
            }

            handler.post(() -> {
                if (!isAdded()) return;
                viewModel.setImages(imageList);
                viewModel.setVideos(videoList);
                finalizeLoading();
            });
        });
    }

    private boolean isImage(String name) {
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".png") || name.endsWith(".webp");
    }

    private boolean isVideo(String name) {
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp");
    }

    private void finalizeLoading() {
        handler.postDelayed(() -> {
            if (isAdded()) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                updateEmptyState();
            }
        }, 250);
    }

    private void stopLoadingUI() {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        updateEmptyState();
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
        if (imageAdapter != null) imageAdapter.shutdown();
        if (videoAdapter != null) videoAdapter.shutdown();
        if (executor != null) executor.shutdownNow();
        super.onDestroyView();
    }
}