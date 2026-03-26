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

        // 1. Initial Setup
        executor = Executors.newFixedThreadPool(2);
        viewModel = new ViewModelProvider(requireActivity()).get(ImagesAndVideoViewModel.class);
        glide = Glide.with(this);

        initViews(view);

        // 2. 🔥 Sabse pehle Argument check karein
        boolean isVideoDirect = getArguments() != null && getArguments().getBoolean("showVideos", false);

        // 3. 🔥 Setup Tabs (Selection logic ko tab creation ke waqt hi handle karein)
        if (tabLayout.getTabCount() == 0) {
            tabLayout.addTab(tabLayout.newTab().setText("Images"), !isVideoDirect);
            tabLayout.addTab(tabLayout.newTab().setText("Videos"), isVideoDirect);
        }

        // 4. 🔥 Visibility Force Apply (SetupTabs se pehle ya baad, ye fixed rahega)
        if (isVideoDirect) {
            recyclerImages.setVisibility(View.GONE);
            recyclerVideos.setVisibility(View.VISIBLE);
        } else {
            recyclerImages.setVisibility(View.VISIBLE);
            recyclerVideos.setVisibility(View.GONE);
        }

        // 5. Baaki heavy operations
        initAdapters();
        loadStatusFolderUri();

        // SetupTabs ko check karein ke isme redundant visibility logic na ho
        setupTabs();

        setupSwipeRefresh();
        observeViewModel();

        if (lottieEmptyState != null) {
            lottieEmptyState.setCacheComposition(true);
        }

        // 6. Data Load
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

                        // Sab ko lowercase kar dein taake .MP4 aur .mp4 dono mil saken
                        String nameLower = n.toLowerCase();

                        if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg") ||
                                nameLower.endsWith(".png") || nameLower.endsWith(".webp")) {
                            images.add(file);
                        }
                        // Yahan videos ki extensions check ho rahi hain
//                        else if (nameLower.endsWith(".mp4") || nameLower.endsWith(".mkv") ||
//                                nameLower.endsWith(".3gp") || nameLower.endsWith(".avi") ||
//                                nameLower.endsWith(".mov")) {
//                            videos.add(file);

                        //}
                        else if (nameLower.endsWith(".mp4")) {
                            videos.add(file);
                            android.util.Log.d("STATUS_DEBUG", "Video Found: " + n);
                        }
                    }
                }
            }

            handler.post(() -> {
                if (!isAdded()) return;
                viewModel.setImages(images);
                viewModel.setVideos(videos);
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                // 🔥 Refresh ke baad empty state update karein
                updateEmptyState();
            });
        });
    }
    private void updateEmptyState() {
        int selectedTab = tabLayout.getSelectedTabPosition();
        boolean isVideoTab = (selectedTab == 1);

        // ViewModel se latest data uthayein
        List<DocumentFile> currentList = isVideoTab ? viewModel.getVideos().getValue() : viewModel.getImages().getValue();
        boolean isEmpty = (currentList == null || currentList.isEmpty());

        if (isEmpty) {
            // FAST UI: Pehle recyclers ko hide karein
            recyclerImages.setVisibility(View.GONE);
            recyclerVideos.setVisibility(View.GONE);

            // Text foran set karein taake delay na lage
            String msg = isVideoTab ?
                    "No Videos Found!\nWatch status on WhatsApp first." :
                    "No Images Found!\nWatch status on WhatsApp first.";
            tvEmptyMessage.setText(msg);

            // Main container aur internal views ko ek sath dikhayen
            emptyStateLayout.setVisibility(View.VISIBLE);
            tvEmptyMessage.setVisibility(View.VISIBLE);
            lottieEmptyState.setVisibility(View.VISIBLE);

            // Performance Fix: Animation tabhi play karein agar list empty ho
            if (!lottieEmptyState.isAnimating()) {
                lottieEmptyState.setAnimation(R.raw.empty_status);
                lottieEmptyState.playAnimation();
            }
        } else {
            // Data milte hi layout hide karein
            emptyStateLayout.setVisibility(View.GONE);
            lottieEmptyState.cancelAnimation();

            // Smooth Switch: Sirf wo recycler dikhayen jo active hai
            if (isVideoTab) {
                if (recyclerVideos.getVisibility() != View.VISIBLE) {
                    recyclerVideos.setVisibility(View.VISIBLE);
                    recyclerImages.setVisibility(View.GONE);
                }
            } else {
                if (recyclerImages.getVisibility() != View.VISIBLE) {
                    recyclerImages.setVisibility(View.VISIBLE);
                    recyclerVideos.setVisibility(View.GONE);
                }
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