package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
    private SavedFilesDB savedFilesDB;
    private final java.util.concurrent.ExecutorService executorService = java.util.concurrent.Executors.newSingleThreadExecutor();
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        lottieEmptyState = view.findViewById(R.id.lottieEmptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);

        if (lottieEmptyState != null) {
            lottieEmptyState.setAnimation(R.raw.empty_status);
            lottieEmptyState.setCacheComposition(true);
        }
        recyclerView.setHasFixedSize(true); // Isse scroll performance 30% barh jati hai
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));


        savedFilesDB = new SavedFilesDB(requireContext());
        DownloadStatsManager statsManager = new DownloadStatsManager(requireContext(), savedFilesDB);

        // Adapter initialization
        adapter = new DownloadAdapter(
                getContext(),
                mediaUris,
                isVideoList,
                uri -> { /* Optional callback */ },
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

        if (mediaUris.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            lottieEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setVisibility(View.VISIBLE);
            if (!lottieEmptyState.isAnimating()) lottieEmptyState.playAnimation();
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyMessage.setVisibility(View.GONE);
            lottieEmptyState.setVisibility(View.GONE);
            lottieEmptyState.pauseAnimation();
        }
    }

    private void loadStatusSaverMedia() {
        // UI thread ko block nahi karega
        executorService.execute(() -> {
            List<Uri> tempUris = new ArrayList<>();
            List<Boolean> tempIsVideo = new ArrayList<>();

            Context context = getContext();
            if (context == null) return;

            // Dono ko ek sath fetch karein
            loadMediaFromMediaStore(context, true, tempUris, tempIsVideo);  // Images
            loadMediaFromMediaStore(context, false, tempUris, tempIsVideo); // Videos

            // UI update hamesha main thread par hogi
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    mediaUris.clear();
                    isVideoList.clear();
                    mediaUris.addAll(tempUris);
                    isVideoList.addAll(tempIsVideo);

                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    updateEmptyMessage();
                });
            }
        });
    }

    // Projection aur Selection ko optimized rakhein
    private void loadMediaFromMediaStore(Context context, boolean isImage, List<Uri> uriList, List<Boolean> videoList) {
        Uri contentUri = isImage ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = { MediaStore.MediaColumns._ID };
        // Faster Query: Sirf folder check karein
        String selection = MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = { "%Status Saver%" };
        String sortOrder = MediaStore.MediaColumns.DATE_ADDED + " DESC";

        try (Cursor cursor = context.getContentResolver().query(contentUri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    uriList.add(android.content.ContentUris.withAppendedId(contentUri, id));
                    videoList.add(!isImage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMediaFromMediaStore(Context context, boolean isImage) {
        Uri contentUri = isImage ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = { MediaStore.MediaColumns._ID };

        // "Status Saver" folder wali files filter karne ke liye
        String selection = MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = { "%Status Saver%" };

        String sortOrder = MediaStore.MediaColumns.DATE_ADDED + " DESC";

        try (Cursor cursor = context.getContentResolver().query(contentUri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri uri = android.content.ContentUris.withAppendedId(contentUri, id);
                    mediaUris.add(uri);
                    isVideoList.add(!isImage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}