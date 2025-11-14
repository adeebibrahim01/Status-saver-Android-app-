package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import androidx.navigation.Navigation;

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

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull android.view.LayoutInflater inflater,
                                          @Nullable android.view.ViewGroup container,
                                          @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_imagesandvideo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
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

        adapter = new ImagesAndVideoAdapter(
                getContext(),
                imageList,
                false,
                this::saveToStatusSaver,
                documentFile -> {
                    boolean isVideo = documentFile.getName() != null &&
                            documentFile.getName().toLowerCase().matches(".*\\.(mp4|mkv|3gp)$");
                    Bundle args = new Bundle();
                    args.putBoolean("showVideos", isVideo);
                    Navigation.findNavController(galleryRecycler).navigate(R.id.nav_gallery, args);
                }
        );
        galleryRecycler.setAdapter(adapter);

        // Check initial tab
        boolean showVideoTab = getArguments() != null && getArguments().getBoolean("showVideos", false);
        TabLayout.Tab initialTab = tabLayout.getTabAt(showVideoTab ? 1 : 0);
        if (initialTab != null) initialTab.select();

        loadStatuses();
        adapter.updateData(showVideoTab ? videoList : imageList, showVideoTab);
        updateActionBarTitle(showVideoTab);
        updateEmptyState();

        // Tab listener
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
            galleryRecycler.setVisibility(android.view.View.GONE);
            emptyStateLayout.setVisibility(android.view.View.VISIBLE);
            lottieEmptyState.setVisibility(android.view.View.VISIBLE);
            lottieEmptyState.setAnimation(R.raw.empty_status);
            lottieEmptyState.buildDrawingCache();
            lottieEmptyState.playAnimation();

            tvEmptyMessage.setText(showVideoTab ?
                    "Videos not available. Please watch complete status from WhatsApp" :
                    "Images not available. Please watch complete status from WhatsApp");
            tvEmptyMessage.setVisibility(android.view.View.VISIBLE);
        } else {
            galleryRecycler.setVisibility(android.view.View.VISIBLE);
            emptyStateLayout.setVisibility(android.view.View.GONE);
            lottieEmptyState.pauseAnimation();
            lottieEmptyState.setVisibility(android.view.View.GONE);
            tvEmptyMessage.setVisibility(android.view.View.GONE);
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

    private void saveToStatusSaver(DocumentFile file) {
        try {
            String fileName = file.getName();
            if (fileName == null) fileName = "status_" + System.currentTimeMillis();

            String mimeType;
            if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".3gp")) {
                mimeType = "video/mp4";
            } else {
                mimeType = "image/jpeg";
                if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && !fileName.endsWith(".png")) {
                    fileName += ".jpg";
                }
            }

            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType);
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_PICTURES + "/Status Saver");

                uri = requireContext().getContentResolver().insert(
                        mimeType.startsWith("video") ?
                                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI :
                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                java.io.File folder = new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "Status Saver");
                if (!folder.exists()) folder.mkdirs();
                java.io.File outFile = new java.io.File(folder, fileName);
                uri = Uri.fromFile(outFile);
            }

            try (java.io.InputStream in = requireContext().getContentResolver().openInputStream(file.getUri());
                 java.io.OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            if (uri != null) {
                requireContext().sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            }

            Toast.makeText(getContext(), "Saved to Status Saver ✅", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to save file ❌", Toast.LENGTH_SHORT).show();
        }
    }
}
