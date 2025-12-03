package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.tabs.TabLayout;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
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

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tabLayout = view.findViewById(R.id.tabLayout);
        galleryRecycler = view.findViewById(R.id.galleryRecycler);
        galleryRecycler.setLayoutManager(new GridLayoutManager(getContext(), 3));
        galleryRecycler.setHasFixedSize(true);

        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        lottieEmptyState = view.findViewById(R.id.lottieEmptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadStatuses();
            swipeRefreshLayout.setRefreshing(false);
        });

        tabLayout.addTab(tabLayout.newTab().setText("Images"));
        tabLayout.addTab(tabLayout.newTab().setText("Videos"));

        loadStatusFolderUri();

        adapter = new ImagesAndVideoAdapter(getContext(), imageList, false);
        galleryRecycler.setAdapter(adapter);

        boolean showVideoTab = getArguments() != null && getArguments().getBoolean("showVideos", false);
        TabLayout.Tab initialTab = tabLayout.getTabAt(showVideoTab ? 1 : 0);
        if (initialTab != null) initialTab.select();

        loadStatuses();
        adapter.updateData(showVideoTab ? videoList : imageList, showVideoTab);
        updateActionBarTitle(showVideoTab);
        updateEmptyState();

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

    private void loadStatusFolderUri() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String uriStr = prefs.getString("statusFolderUri", null);
        if (uriStr != null) {
            statusFolderUri = Uri.parse(uriStr);
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            File folder = new File("/storage/emulated/0/WhatsApp/Media/.Statuses");
            if (folder.exists() && folder.isDirectory()) statusFolderUri = Uri.fromFile(folder);
        }
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
            lottieEmptyState.setVisibility(View.VISIBLE);
            lottieEmptyState.setAnimation(R.raw.empty_status);
            lottieEmptyState.buildDrawingCache();
            lottieEmptyState.playAnimation();

            tvEmptyMessage.setText(showVideoTab ?
                    "Videos not available. Please watch complete status from WhatsApp" :
                    "Images not available. Please watch complete status from WhatsApp");
            tvEmptyMessage.setVisibility(View.VISIBLE);
        } else {
            galleryRecycler.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            lottieEmptyState.pauseAnimation();
            lottieEmptyState.setVisibility(View.GONE);
            tvEmptyMessage.setVisibility(View.GONE);
        }
    }

    private void loadStatuses() {
        imageList.clear();
        videoList.clear();

        if (statusFolderUri == null) return;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            File folder = new File(statusFolderUri.getPath());
            if (!folder.exists() || !folder.isDirectory()) return;
            addFilesFromFolder(folder);
        } else {
            DocumentFile folder = DocumentFile.fromTreeUri(requireContext(), statusFolderUri);
            if (folder == null || !folder.exists() || !folder.isDirectory()) return;
            addFilesFromFolder(folder);
        }

        boolean showVideoTab = tabLayout.getSelectedTabPosition() == 1;
        adapter.updateData(showVideoTab ? videoList : imageList, showVideoTab);
        updateEmptyState();
    }

    private void addFilesFromFolder(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")) {
                    imageList.add(DocumentFile.fromFile(file));
                } else if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp")) {
                    videoList.add(DocumentFile.fromFile(file));
                }
            } else if (file.isDirectory()) {
                addFilesFromFolder(file);
            }
        }
    }

    private void addFilesFromFolder(DocumentFile folder) {
        for (DocumentFile file : folder.listFiles()) {
            if (file.isFile()) {
                String name = file.getName() != null ? file.getName().toLowerCase() : "";
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")) {
                    imageList.add(file);
                } else if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp")) {
                    videoList.add(file);
                }
            } else if (file.isDirectory()) {
                addFilesFromFolder(file);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) adapter.shutdownScheduler();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatuses();
        if (adapter != null) adapter.notifyDataSetChanged();
    }
}
