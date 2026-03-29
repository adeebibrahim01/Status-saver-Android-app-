package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.Home.DownloadStatsManager;
import com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo.SavedFilesDB;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DownloadAdapter adapter;
    private final List<Uri> mediaUris = new ArrayList<>();
    private final List<Boolean> isVideoList = new ArrayList<>();
    private TextView tvEmptyMessage;
    private LottieAnimationView lottieEmptyState;
    private DownloadViewModel viewModel; // ✅ class-level variable
    private SavedFilesDB savedFilesDB;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        lottieEmptyState = view.findViewById(R.id.lottieEmptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);

        // 🔥 LOTTIE OPTIMIZATION: Animation ko pehle hi load aur cache kar lo
        if (lottieEmptyState != null) {
            lottieEmptyState.setAnimation(R.raw.empty_status); // Check karein file name sahi hai
            lottieEmptyState.setCacheComposition(true);
        }

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setHasFixedSize(true);

        savedFilesDB = new SavedFilesDB(requireContext());
        DownloadStatsManager statsManager = new DownloadStatsManager(requireContext(), savedFilesDB);

        adapter = new DownloadAdapter(
                getContext(),
                mediaUris,
                isVideoList,
                uri -> { /* Handle deletion if needed */ },
                this::updateEmptyMessage,
                statsManager,
                savedFilesDB
        );

        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadStatusSaverMedia();
            swipeRefreshLayout.setRefreshing(false);
        });

        loadStatusSaverMedia();

        return view;
    }



    @Override
    public void onResume() {
        super.onResume();
        loadStatusSaverMedia();

    }

    private void updateEmptyMessage() {
        if (!isAdded()) return;

        if (mediaUris == null || mediaUris.isEmpty()) {
            // Data nahi hai toh recycler hide karein
            recyclerView.setVisibility(View.GONE);

            // Lottie aur Message show karein
            lottieEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setVisibility(View.VISIBLE);

            // Agar animation ruki hui hai toh start karein
            if (!lottieEmptyState.isAnimating()) {
                lottieEmptyState.playAnimation();
            }
        } else {
            // Data hai toh sab hide kar ke recycler dikhayein
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyMessage.setVisibility(View.GONE);
            lottieEmptyState.setVisibility(View.GONE);
            lottieEmptyState.pauseAnimation();
        }
    }


    private void loadStatusSaverMedia() {
        mediaUris.clear();
        isVideoList.clear();
        Context context = getContext();
        if (context == null) return;

        // 🔥 FIX: Android 9 aur Android 10+ dono ke liye Hybrid Logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10, 11, 12, 13, 14+ (MediaStore logic)
            loadImages(context);
            loadVideos(context);
        } else {
            // Android 9 aur usse neeche (Direct File Scanning)
            loadLegacyMedia();
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateEmptyMessage();
    }

    // Android 9 ke liye naya method
    private void loadLegacyMedia() {
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Status Saver");
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                // Sort by Date (Newest first)
                java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

                for (File file : files) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".mp4") || name.endsWith(".mkv")) {
                        mediaUris.add(Uri.fromFile(file));
                        isVideoList.add(name.endsWith(".mp4") || name.endsWith(".mkv"));
                    }
                }
            }
        }
    }

    private void loadImages(Context context) {
        Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.MediaColumns._ID };
        String selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = { "%Status Saver%" };

        try (Cursor cursor = context.getContentResolver().query(imagesUri, projection, selection, selectionArgs,
                MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = Uri.withAppendedPath(imagesUri, String.valueOf(id));
                    mediaUris.add(contentUri);
                    isVideoList.add(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadVideos(Context context) {
        Uri videosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.MediaColumns._ID };
        String selection = MediaStore.Video.Media.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = { "%Status Saver%" };

        try (Cursor cursor = context.getContentResolver().query(videosUri, projection, selection, selectionArgs,
                MediaStore.Video.Media.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = Uri.withAppendedPath(videosUri, String.valueOf(id));
                    mediaUris.add(contentUri);
                    isVideoList.add(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
