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

    private RecyclerView galleryRecycler;
    private TabLayout tabLayout;
    private ImagesAndVideoAdapter adapter;
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

    private static final long MIN_PROGRESS_DURATION = 2000;

    // 🔐 TAB REQUEST CONTROL (MAIN FIX)
    private int tabRequestId = 0;

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

        executor = Executors.newFixedThreadPool(3);
        viewModel = new ViewModelProvider(requireActivity()).get(ImagesAndVideoViewModel.class);
        glide = Glide.with(this);

        initViews(view);
        loadStatusFolderUri();
        setupTabs();
        setupSwipeRefresh();

        showLoading(this::loadStatuses);

        adapter = new ImagesAndVideoAdapter(
                requireContext(),
                new ArrayList<>(),
                false,
                glide,
                this::hideProgressAndShowRecycler
        );

        galleryRecycler.setAdapter(adapter);
        galleryRecycler.setItemViewCacheSize(20);
        ((GridLayoutManager) galleryRecycler.getLayoutManager())
                .setInitialPrefetchItemCount(3);

        boolean showVideoTab = getArguments() != null &&
                getArguments().getBoolean("showVideos", false);
        selectTab(showVideoTab ? 1 : 0);

        observeViewModel();
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tabLayout = view.findViewById(R.id.tabLayout);
        galleryRecycler = view.findViewById(R.id.galleryRecycler);
        galleryRecycler.setLayoutManager(new GridLayoutManager(getContext(), 3));
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        lottieEmptyState = view.findViewById(R.id.lottieEmptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        progressBar = view.findViewById(R.id.progressBarLoading);
    }

    private void observeViewModel() {

        viewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            imageList.clear();
            imageList.addAll(images);

            if (tabLayout.getSelectedTabPosition() == 0) {
                adapter.updateDataAsync(imageList, false);
            }
            updateEmptyState();
        });

        viewModel.getVideos().observe(getViewLifecycleOwner(), videos -> {
            videoList.clear();
            videoList.addAll(videos);

            if (tabLayout.getSelectedTabPosition() == 1) {
                adapter.updateDataAsync(videoList, true);
            }
            updateEmptyState();
        });
    }

    private void setupTabs() {

        tabLayout.addTab(tabLayout.newTab().setText("Images"));
        tabLayout.addTab(tabLayout.newTab().setText("Videos"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                final int myRequestId = ++tabRequestId;
                final boolean showVideo = tab.getPosition() == 1;
                final List<DocumentFile> list = showVideo ? videoList : imageList;

                // 🔥 Show loader ONLY if list is empty
                if (list.isEmpty()) {
                    progressBar.setVisibility(View.VISIBLE);
                    galleryRecycler.setVisibility(View.GONE);
                    emptyStateLayout.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.GONE);
                    galleryRecycler.setVisibility(View.VISIBLE);
                }

                safeExecute(() -> {
                    adapter.updateDataAsync(list, showVideo);

                    // Post UI update on main thread
                    handler.post(() -> {
                        if (!isAdded() || myRequestId != tabRequestId) return;

                        progressBar.setVisibility(View.GONE);
                        updateEmptyState();
                    });
                });
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }


    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(
                () -> showLoading(this::loadStatuses)
        );
    }

    private void loadStatusFolderUri() {
        Context ctx = requireContext();
        String uri = ctx.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("statusFolderUri", null);
        statusFolderUri = uri != null ? Uri.parse(uri) : null;
    }

    private void selectTab(int index) {
        if (tabLayout.getTabAt(index) != null)
            tabLayout.getTabAt(index).select();
    }

    private void loadStatuses() {
        if (statusFolderUri == null || executor == null || executor.isShutdown())
            return;

        safeExecute(() -> {

            DocumentFile folder =
                    DocumentFile.fromTreeUri(requireContext(), statusFolderUri);
            if (folder == null || !folder.isDirectory()) return;

            List<DocumentFile> images = new ArrayList<>();
            List<DocumentFile> videos = new ArrayList<>();
            scanFolder(folder, images, videos);

            handler.post(() -> {
                if (isAdded()) {
                    viewModel.setImages(images);
                    viewModel.setVideos(videos);
                }
            });
        });
    }

    private void scanFolder(DocumentFile folder,
                            List<DocumentFile> images,
                            List<DocumentFile> videos) {

        for (DocumentFile file : folder.listFiles()) {
            if (file.isFile()) addFile(file, images, videos);
            else if (file.isDirectory())
                scanFolder(file, images, videos);
        }
    }

    private void addFile(DocumentFile file,
                         List<DocumentFile> images,
                         List<DocumentFile> videos) {

        if (file.getName() == null) return;
        String n = file.getName().toLowerCase();

        if (n.endsWith(".jpg") || n.endsWith(".jpeg")
                || n.endsWith(".png") || n.endsWith(".webp"))
            images.add(file);
        else if (n.endsWith(".mp4") || n.endsWith(".mkv")
                || n.endsWith(".3gp"))
            videos.add(file);
    }

    private void updateEmptyState() {

        boolean showVideo = tabLayout.getSelectedTabPosition() == 1;
        List<DocumentFile> current = showVideo ? videoList : imageList;

        boolean empty = current.isEmpty();
        galleryRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
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

    private void showLoading(Runnable task) {

        long start = System.currentTimeMillis();
        progressBar.setVisibility(View.VISIBLE);
        galleryRecycler.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        // Ensure SwipeRefreshLayout spinner is visible
        swipeRefreshLayout.setRefreshing(true);

        safeExecute(() -> {
            if (task != null) task.run();

            long elapsed = System.currentTimeMillis() - start;
            long delay = Math.max(0, MIN_PROGRESS_DURATION - elapsed);

            handler.postDelayed(() -> {
                hideProgressAndShowRecycler();
                swipeRefreshLayout.setRefreshing(false); // stop the spinner
            }, delay);
        });
    }


    private void hideProgressAndShowRecycler() {
        progressBar.setVisibility(View.GONE);
        galleryRecycler.setVisibility(View.VISIBLE);
        updateEmptyState();
    }

    private void safeExecute(Runnable r) {
        try {
            if (executor != null && !executor.isShutdown())
                executor.execute(r);
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) adapter.shutdownScheduler();
        if (executor != null && !executor.isShutdown())
            executor.shutdownNow();
        executor = null;
    }
}
