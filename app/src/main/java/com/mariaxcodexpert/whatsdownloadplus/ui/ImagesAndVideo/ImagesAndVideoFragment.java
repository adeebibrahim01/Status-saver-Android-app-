package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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
import com.google.android.material.tabs.TabLayout;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class ImagesAndVideoFragment extends Fragment {

    private RecyclerView galleryRecycler;
    private TabLayout tabLayout;
    private ImagesAndVideoAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ConstraintLayout emptyStateLayout;
    private LottieAnimationView lottieEmptyState;
    private TextView tvEmptyMessage;

    private final List<DocumentFile> imageList = new ArrayList<>();
    private final List<DocumentFile> videoList = new ArrayList<>();
    private Uri statusFolderUri;
    private ImagesAndVideoViewModel viewModel;

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

        // Use proper ViewModelProvider call
        viewModel = new ViewModelProvider(requireActivity()).get(ImagesAndVideoViewModel.class);

        initViews(view);
        loadStatusFolderUri();
        setupTabs();
        setupSwipeRefresh();

        adapter = new ImagesAndVideoAdapter(requireContext(), imageList, false);
        galleryRecycler.setAdapter(adapter);

        boolean showVideoTab = getArguments() != null && getArguments().getBoolean("showVideos", false);
        selectTab(showVideoTab ? 1 : 0);

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            imageList.clear();
            imageList.addAll(images);
            if (tabLayout.getSelectedTabPosition() == 0) adapter.updateData(imageList, false);
            updateEmptyState();
        });

        viewModel.getVideos().observe(getViewLifecycleOwner(), videos -> {
            videoList.clear();
            videoList.addAll(videos);
            if (tabLayout.getSelectedTabPosition() == 1) adapter.updateData(videoList, true);
            updateEmptyState();
        });
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tabLayout = view.findViewById(R.id.tabLayout);
        galleryRecycler = view.findViewById(R.id.galleryRecycler);
        galleryRecycler.setLayoutManager(new GridLayoutManager(getContext(), 3));
        galleryRecycler.setHasFixedSize(true);

        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        lottieEmptyState = view.findViewById(R.id.lottieEmptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Images"));
        tabLayout.addTab(tabLayout.newTab().setText("Videos"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean showVideo = tab.getPosition() == 1;
                adapter.updateData(showVideo ? videoList : imageList, showVideo);
                updateActionBarTitle(showVideo);
                updateEmptyState();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadStatuses();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadStatusFolderUri() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String uriStr = prefs.getString("statusFolderUri", null);
        if (uriStr != null) statusFolderUri = Uri.parse(uriStr);
        else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            File folder = new File("/storage/emulated/0/WhatsApp/Media/.Statuses");
            if (folder.exists() && folder.isDirectory()) statusFolderUri = Uri.fromFile(folder);
        }
    }

    private void selectTab(int index) {
        TabLayout.Tab tab = tabLayout.getTabAt(index);
        if (tab != null) tab.select();
        loadStatuses();
    }

    private void loadStatuses() {
        List<DocumentFile> images = new ArrayList<>();
        List<DocumentFile> videos = new ArrayList<>();
        if (statusFolderUri == null) return;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
            scanFolder(new File(statusFolderUri.getPath()), images, videos);
        else
            scanFolder(DocumentFile.fromTreeUri(requireContext(), statusFolderUri), images, videos);

        viewModel.setImages(images);
        viewModel.setVideos(videos);
    }

    private void scanFolder(Object folder, List<DocumentFile> images, List<DocumentFile> videos) {
        if (folder instanceof File) {
            File f = (File) folder;
            File[] files = f.listFiles();
            if (files == null) return;
            for (File file : files) {
                if (file.isFile()) {
                    if (isImageFile(file.getName())) images.add(DocumentFile.fromFile(file));
                    else if (isVideoFile(file.getName())) videos.add(DocumentFile.fromFile(file));
                } else if (file.isDirectory()) scanFolder(file, images, videos);
            }
        } else if (folder instanceof DocumentFile) {
            DocumentFile df = (DocumentFile) folder;
            for (DocumentFile file : df.listFiles()) {
                if (file.isFile()) {
                    String name = file.getName() != null ? file.getName() : "";
                    if (isImageFile(name)) images.add(file);
                    else if (isVideoFile(name)) videos.add(file);
                } else if (file.isDirectory()) scanFolder(file, images, videos);
            }
        }
    }

    private boolean isImageFile(String name) {
        if (name == null) return false;
        name = name.toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
    }

    private boolean isVideoFile(String name) {
        if (name == null) return false;
        name = name.toLowerCase();
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp");
    }

    private void updateActionBarTitle(boolean showVideo) {
        if (getActivity() != null) {
            String title = showVideo ? "Videos" : "Images";
            ((androidx.appcompat.app.AppCompatActivity) getActivity()).getSupportActionBar().setTitle(title);
        }
    }

    private void updateEmptyState() {
        boolean showVideoTab = tabLayout.getSelectedTabPosition() == 1;
        List<DocumentFile> currentList = showVideoTab ? videoList : imageList;

        if (currentList.isEmpty()) {
            galleryRecycler.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);

            // Show animation
            lottieEmptyState.setVisibility(View.VISIBLE);
            lottieEmptyState.setAnimation(R.raw.empty_status);
            lottieEmptyState.playAnimation();

            // Show proper text
            tvEmptyMessage.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText(showVideoTab
                    ? "Videos not available 😔\nPlease check WhatsApp status first"
                    : "Images not available 😔\nPlease check WhatsApp status first");

        } else {
            galleryRecycler.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);

            lottieEmptyState.pauseAnimation();
            lottieEmptyState.setVisibility(View.GONE);
            tvEmptyMessage.setVisibility(View.GONE);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) adapter.shutdownScheduler();
    }
}
