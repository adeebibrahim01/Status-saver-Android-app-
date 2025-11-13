package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mariaxcodexpert.whatsdownloadplus.Permissions.PermissionsActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.util.ArrayList;
import java.util.List;

public class DownloadFragmentModern extends Fragment {

    private RecyclerView recyclerView;
    private DownloadAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyMessage;

    private final List<Uri> mediaUris = new ArrayList<>();
    private final List<Boolean> isVideoList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_download, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        emptyMessage = view.findViewById(R.id.tvEmptyMessage);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setHasFixedSize(true);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadStatusSaverMedia();
            swipeRefreshLayout.setRefreshing(false);
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null && activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Download");
        }

        loadStatusSaverMedia();

        PermissionsActivity permissionsActivity = null;
        if (getActivity() instanceof PermissionsActivity) {
            permissionsActivity = (PermissionsActivity) getActivity();
        }

        adapter = new DownloadAdapter(getContext(), mediaUris, isVideoList, permissionsActivity, this::deleteFile);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatusSaverMedia();

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null && activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Download");
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    // ===== Load videos/images via MediaStore (Android ≥ 10) =====
    private void loadStatusSaverMedia() {
        mediaUris.clear();
        isVideoList.clear();

        Context context = getContext();
        if (context == null) return;

        loadImages(context);
        loadVideos(context);

        updateEmptyView();
        if (adapter != null) adapter.notifyDataSetChanged();
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
                    Uri contentUri = ContentUris.withAppendedId(imagesUri, id);
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
                    Uri contentUri = ContentUris.withAppendedId(videosUri, id);
                    mediaUris.add(contentUri);
                    isVideoList.add(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== Delete media via MediaStore =====
    private void deleteFile(int position) {
        if (position < 0 || position >= mediaUris.size()) return;

        Uri uri = mediaUris.get(position);

        try {
            int deleted = requireContext().getContentResolver().delete(uri, null, null);
            if (deleted > 0) {
                mediaUris.remove(position);
                isVideoList.remove(position);
                adapter.notifyItemRemoved(position);
                updateEmptyView();
                Toast.makeText(getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error deleting file", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Empty view handling =====
    private void updateEmptyView() {
        if (mediaUris.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            if (emptyMessage != null) emptyMessage.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            if (emptyMessage != null) emptyMessage.setVisibility(View.GONE);
        }
    }
}
