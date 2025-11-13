package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadFragmentLegacy extends Fragment {

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

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // ===== Load media from legacy folder =====
    private void loadStatusSaverMedia() {
        mediaUris.clear();
        isVideoList.clear();

        File folder = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Status Saver");

        if (folder.exists() && folder.isDirectory()) {
            addFilesFromFolder(folder);
        }

        updateEmptyView();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    // ===== Recursively add files from folder =====
    private void addFilesFromFolder(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                String name = file.getName().toLowerCase();
                Uri uri = Uri.fromFile(file);

                if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp")) {
                    mediaUris.add(uri);
                    isVideoList.add(true);
                } else if (name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                        name.endsWith(".png") || name.endsWith(".webp")) {
                    mediaUris.add(uri);
                    isVideoList.add(false);
                }
            } else if (file.isDirectory()) {
                addFilesFromFolder(file);
            }
        }
    }

    // ===== Delete file from storage and update adapter =====
    private void deleteFile(int position) {
        if (position < 0 || position >= mediaUris.size()) return;

        Uri uri = mediaUris.get(position);
        File file = new File(uri.getPath());

        if (file.exists() && file.delete()) {
            mediaUris.remove(position);
            isVideoList.remove(position);
            adapter.notifyItemRemoved(position);
            updateEmptyView();
            Toast.makeText(getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Show or hide empty view =====
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
