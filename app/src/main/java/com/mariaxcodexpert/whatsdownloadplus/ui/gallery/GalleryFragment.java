package com.mariaxcodexpert.whatsdownloadplus.ui.gallery;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.Helper.SmartNotify;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;
import com.mariaxcodexpert.whatsdownloadplus.ui.preview.MediaPreviewActivity;

import java.util.List;

public class GalleryFragment extends Fragment {

    private GalleryViewModel viewModel;
    private GalleryAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private View emptyLayout;
    private ProgressBar progressBar;
    private TabLayout tabLayout;

    private boolean isShowingVideo = false;
    private static final String PREF_NAME = "DownloadPrefs";
    private static final String KEY_ALWAYS_ORIGINAL = "always_original";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_imagesandvideo, container, false);

        isShowingVideo = getArguments() != null && getArguments().getBoolean("arg_is_video", false);

        tabLayout = view.findViewById(R.id.tabLayout);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyLayout = view.findViewById(R.id.emptyStateLayout);
        progressBar = view.findViewById(R.id.progressBar);

        setupViewModel();
        setupRecyclerView();
        setupTabs();

        swipeRefresh.setOnRefreshListener(this::refreshData);

        // Fragment load hotay hi data fetch karne ke liye silentRefresh call kiya hai
        silentRefresh();

        return view;
    }

    private void setupTabs() {
        if (tabLayout.getTabCount() > 0) tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_images)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_videos)));

        TabLayout.Tab targetTab = tabLayout.getTabAt(isShowingVideo ? 1 : 0);
        if (targetTab != null) targetTab.select();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isShowingVideo = (tab.getPosition() == 1);

                if (adapter != null) {
                    adapter.submitList(null);
                }

                recyclerView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                emptyLayout.setVisibility(View.GONE);

                silentRefresh();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(GalleryViewModel.class);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || getView() == null) return;

            if (swipeRefresh != null) swipeRefresh.setRefreshing(state.isRefreshing);

            boolean isValidData = true;
            if (state.data != null && !state.data.isEmpty()) {
                Object firstItem = state.data.get(0);
                boolean isVideoData = (firstItem instanceof VideoEntity);
                if (isShowingVideo != isVideoData) isValidData = false;
            }

            if (state.isLoading && (state.data == null || state.data.isEmpty())) {
                showLoadingState();
            } else {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (state.showEmpty || !isValidData) {
                    if (state.showEmpty) showEmptyState();
                } else {
                    showDataState(state.data);
                }
            }
        });
    }

    private void showLoadingState() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (emptyLayout != null) {
            emptyLayout.setVisibility(View.VISIBLE);
            setupEmptyText();
        }
    }

    private void showDataState(List<Object> data) {
        if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);
        if (recyclerView != null) {
            if (recyclerView.getVisibility() != View.VISIBLE) {
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.scheduleLayoutAnimation();
            }
            if (adapter != null) adapter.submitList(data);
        }
    }

    private void setupRecyclerView() {
        adapter = new GalleryAdapter(Glide.with(this), viewModel,
                this::handlePreviewWithAd,
                (item, holder, uri, isVid, name, isDown) -> {
                    if (isDown) { SmartNotify.success(getView(), getString(R.string.toast_already_saved)); return; }
                    if (isVid) adapter.handleAdThenDownload(item, holder, uri, name, true);
                    else checkPreferenceAndShowDialog(item, holder, uri, name);
                }
        );
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(adapter);
    }

    private void startLoadProcess(boolean isManual) {
        if (!isAdded() || getContext() == null) return;
        if (swipeRefresh != null) {
            if (!isManual) swipeRefresh.setRefreshing(false);
        }

        try {
            SharedPreferences prefs = getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            String uriStr = prefs.getString("statusFolderUri", null);

            if (uriStr != null && viewModel != null) {
                viewModel.loadStatuses(Uri.parse(uriStr), isManual, isShowingVideo);
            } else {
                if (isManual) SmartNotify.error(getView(), getString(R.string.error_permission_denied));
            }
        } catch (Exception e) {
            Log.e("GALLERY_FRAG", "Error loading process: " + e.getMessage());
        }
    }

    private void setupEmptyText() {
        TextView tvTitle = emptyLayout.findViewById(R.id.tvEmptyTitle);
        TextView tvDesc = emptyLayout.findViewById(R.id.tvEmptyDescription);
        Button btnOpenWA = emptyLayout.findViewById(R.id.btnOpenWhatsApp);
        if (tvTitle != null) {
            tvTitle.setText(isShowingVideo ? getString(R.string.empty_videos_title) : getString(R.string.empty_images_title));
        }
        if (tvDesc != null) {
            tvDesc.setText(getString(R.string.empty_whatsapp_desc));
        }

        if (btnOpenWA != null) {
            btnOpenWA.setOnClickListener(v -> {
                try {
                    Intent intent = getContext().getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                    if (intent != null) {
                        startActivity(intent);
                    } else {
                        Toast.makeText(getContext(), getString(R.string.toast_whatsapp_not_installed), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
    }

    private void handlePreviewWithAd(Object item) {
        AdManager.showInterstitial(getActivity(), new AdManager.AdCallback() {
            @Override public void onAdClosed() { openPreview(item); }
            @Override public void onAdFailed() { openPreview(item); }
        });
    }
    private void refreshData() { startLoadProcess(true); }
    private void silentRefresh() { startLoadProcess(false); }
    private void openPreview(Object item) {
        if (getContext() == null || item == null) return;

        try {
            Intent intent = new Intent(getContext(), MediaPreviewActivity.class);
            String uri = "", name = "";
            boolean isDown = false, isVid = false;

            if (item instanceof ImageEntity) {
                ImageEntity img = (ImageEntity) item;
                uri = img.getUri(); name = img.fileName; isDown = img.isDownloaded; isVid = false;
            } else if (item instanceof VideoEntity) {
                VideoEntity vid = (VideoEntity) item;
                uri = vid.getUri(); name = vid.fileName; isDown = vid.isDownloaded; isVid = true;
            }

            if (uri != null && !uri.isEmpty()) {
                intent.putExtra("EXTRA_URI", uri);
                intent.putExtra("EXTRA_IS_VIDEO", isVid);
                intent.putExtra("EXTRA_FILE_NAME", name);
                intent.putExtra("EXTRA_IS_DOWNLOADED", isDown);
                intent.putExtra("EXTRA_SOURCE", "MAGIC_LAB");
                startActivity(intent);
            }
        } catch (Exception e) {
            SmartNotify.error(getView(), getString(R.string.error_open_preview));
        }
    }
    private void checkPreferenceAndShowDialog(Object item, GalleryAdapter.GalleryViewHolder holder, String uri, String name) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_ALWAYS_ORIGINAL, false)) adapter.handleAdThenDownload(item, holder, uri, name, false);
        else showDownloadChoiceDialog(item, holder, uri, name);
    }
    private void showDownloadChoiceDialog(Object item, GalleryAdapter.GalleryViewHolder holder, String uri, String name) {
        if (getActivity() == null || getActivity().isFinishing()) return;

        try {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_download_choice, null);
            AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(dialogView).create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            CheckBox cbAlwaysOriginal = dialogView.findViewById(R.id.cbDoNotShow);

            dialogView.findViewById(R.id.btnMagicLab).setOnClickListener(v -> {
                dialog.dismiss();
                handlePreviewWithAd(item);
            });

            dialogView.findViewById(R.id.btnOriginal).setOnClickListener(v -> {
                if (cbAlwaysOriginal.isChecked() && getContext() != null) {
                    getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_ALWAYS_ORIGINAL, true).apply();
                }
                dialog.dismiss();
                if (adapter != null) adapter.handleAdThenDownload(item, holder, uri, name, false);
            });

            dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        } catch (Exception e) {
            if (adapter != null) adapter.handleAdThenDownload(item, holder, uri, name, false);
        }
    }
    @Override public void onResume() { super.onResume(); silentRefresh(); }
}