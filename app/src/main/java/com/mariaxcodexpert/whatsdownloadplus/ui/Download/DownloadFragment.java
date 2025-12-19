package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_download, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        lottieEmptyState = view.findViewById(R.id.lottieEmptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);


        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setHasFixedSize(true);

        savedFilesDB = new SavedFilesDB(requireContext()); // initialize DB
// ✅ Reuse savedFilesDB for DownloadStatsManager
        DownloadStatsManager statsManager = new DownloadStatsManager(requireContext(), savedFilesDB);

        adapter = new DownloadAdapter(
                getContext(),
                mediaUris,
                isVideoList,
                uri -> { /* Nothing extra needed */ }, // deletion callback handled in adapter
                this::updateEmptyMessage,
                statsManager,
                savedFilesDB // ✅ Adapter already uses the same DB
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
        if (mediaUris.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            recyclerView.post(() -> {
                lottieEmptyState.setVisibility(View.VISIBLE);
                lottieEmptyState.playAnimation();
                tvEmptyMessage.setVisibility(View.VISIBLE);
            });
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyMessage.setVisibility(View.GONE);
            lottieEmptyState.setVisibility(View.GONE);
            lottieEmptyState.pauseAnimation();
        }

        // Update counts somewhere in your UI
        int todayCount = savedFilesDB.getTodayCount();
        int last7DaysCount = savedFilesDB.getLast7DaysCount();
        // Update your TextViews / counters with these values
    }


    private void loadStatusSaverMedia() {
        mediaUris.clear();
        isVideoList.clear();

        Context context = getContext();
        if (context == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            loadImages(context);
            loadVideos(context);
        }

        adapter.notifyDataSetChanged();
        updateEmptyMessage();
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
