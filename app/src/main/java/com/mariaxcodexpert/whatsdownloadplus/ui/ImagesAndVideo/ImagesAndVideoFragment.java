package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
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
    private final List<DocumentFile> imageList = new ArrayList<>();
    private final List<DocumentFile> videoList = new ArrayList<>();

    private Uri statusFolderUri;
    private ImagesAndVideoViewModel viewModel;
    private ExecutorService executor;
    private RequestManager glide;
    private RecyclerView recyclerImages;
    private RecyclerView recyclerVideos;

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_imagesandvideo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        executor = Executors.newFixedThreadPool(4);
        viewModel = new ViewModelProvider(requireActivity()).get(ImagesAndVideoViewModel.class);
        glide = Glide.with(this);

        initViews(view);
        loadStatusFolderUri();
        setupTabs();
        setupSwipeRefresh();
        initAdapters();
        observeViewModel();

        boolean showVideoTab = getArguments() != null &&
                getArguments().getBoolean("showVideos", false);
        selectTab(showVideoTab ? 1 : 0);

        loadStatuses();
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerImages = view.findViewById(R.id.recyclerImages);
        recyclerVideos = view.findViewById(R.id.recyclerVideos);

        recyclerImages.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerVideos.setLayoutManager(new GridLayoutManager(getContext(), 3));

        recyclerImages.setHasFixedSize(true);
        recyclerVideos.setHasFixedSize(true);

        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        lottieEmptyState = view.findViewById(R.id.lottieEmptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        progressBar = view.findViewById(R.id.progressBarLoading);
    }

    private void initAdapters() {
        // Get your shared SavedFilesDB instance
        SavedFilesDB savedFilesDB = new SavedFilesDB(requireContext());

        // Pass savedFilesDB to adapters
        imageAdapter = new ImagesAndVideoAdapter(requireContext(), glide, false, savedFilesDB);
        videoAdapter = new ImagesAndVideoAdapter(requireContext(), glide, true, savedFilesDB);

        recyclerImages.setAdapter(imageAdapter);
        recyclerVideos.setAdapter(videoAdapter);

        imageAdapter.attachRecyclerView(recyclerImages);
        videoAdapter.attachRecyclerView(recyclerVideos);
    }

    private void observeViewModel() {
        viewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            imageList.clear();
            imageList.addAll(images);
            imageAdapter.submitList(new ArrayList<>(imageList));
        });

        viewModel.getVideos().observe(getViewLifecycleOwner(), videos -> {
            videoList.clear();
            videoList.addAll(videos);
            videoAdapter.submitList(new ArrayList<>(videoList));
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Images"));
        tabLayout.addTab(tabLayout.newTab().setText("Videos"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean showVideo = tab.getPosition() == 1;
                recyclerImages.setVisibility(showVideo ? View.GONE : View.VISIBLE);
                recyclerVideos.setVisibility(showVideo ? View.VISIBLE : View.GONE);
                updateEmptyState();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadStatuses);
    }

    private void loadStatusFolderUri() {
        String uri = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("statusFolderUri", null);
        statusFolderUri = uri != null ? Uri.parse(uri) : null;
    }

    private void selectTab(int index) {
        if (tabLayout.getTabAt(index) != null) tabLayout.getTabAt(index).select();
    }

    private void loadStatuses() {
        if (statusFolderUri == null || executor == null || executor.isShutdown()) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            DocumentFile folder = DocumentFile.fromTreeUri(requireContext(), statusFolderUri);
            if (folder == null || !folder.isDirectory()) return;

            List<DocumentFile> images = new ArrayList<>();
            List<DocumentFile> videos = new ArrayList<>();

            for (DocumentFile file : folder.listFiles()) {
                if (file.isFile()) addFile(file, images, videos);
            }

            handler.post(() -> {
                if (!isAdded()) return;
                viewModel.setImages(images);
                viewModel.setVideos(videos);
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            });
        });
    }

    private void addFile(DocumentFile file, List<DocumentFile> images, List<DocumentFile> videos) {
        String n = file.getName();
        if (n == null) return;
        n = n.toLowerCase();
        if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp")) images.add(file);
        else if (n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".3gp")) videos.add(file);
    }

    private void updateEmptyState() {
        boolean showVideo = tabLayout.getSelectedTabPosition() == 1;
        List<DocumentFile> current = showVideo ? videoList : imageList;
        boolean empty = current.isEmpty();

        recyclerImages.setVisibility(!showVideo && !empty ? View.VISIBLE : View.GONE);
        recyclerVideos.setVisibility(showVideo && !empty ? View.VISIBLE : View.GONE);
        emptyStateLayout.setVisibility(empty ? View.VISIBLE : View.GONE);

        if (empty) {
            lottieEmptyState.setAnimation(R.raw.empty_status);
            lottieEmptyState.playAnimation();
            tvEmptyMessage.setText(showVideo
                    ? "Videos not available 😔\nPlease check WhatsApp status first"
                    : "Images not available 😔\nPlease check WhatsApp status first");
        } else {
            lottieEmptyState.pauseAnimation();
            tvEmptyMessage.setText("");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (imageAdapter != null) imageAdapter.shutdownScheduler();
        if (videoAdapter != null) videoAdapter.shutdownScheduler();
        if (executor != null && !executor.isShutdown()) executor.shutdownNow();
        executor = null;
    }
}
