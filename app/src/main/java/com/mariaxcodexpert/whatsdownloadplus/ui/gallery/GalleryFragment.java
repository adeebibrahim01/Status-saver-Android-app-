package com.mariaxcodexpert.whatsdownloadplus.ui.gallery;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.SmartNotify;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;
import com.mariaxcodexpert.whatsdownloadplus.ui.preview.MediaPreviewActivity;

public class GalleryFragment extends Fragment {

    private GalleryViewModel viewModel;
    private GalleryAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private View emptyLayout;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private TextView Headertext ;

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
        Headertext =view.findViewById(R.id.headertv);
//        proTypewriter(Headertext, "Status Gallery");

        setupViewModel();
        setupRecyclerView();
        setupTabs();

        swipeRefresh.setOnRefreshListener(this::refreshData);

        return view;
    }

    private void setupTabs() {

        if (tabLayout.getTabCount() > 0) tabLayout.removeAllTabs();

        tabLayout.addTab(tabLayout.newTab().setText("Images"));
        tabLayout.addTab(tabLayout.newTab().setText("Videos"));


        TabLayout.Tab targetTab = tabLayout.getTabAt(isShowingVideo ? 1 : 0);
        if (targetTab != null) targetTab.select();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isShowingVideo = (tab.getPosition() == 1);

                if (adapter != null) adapter.submitList(null);

                // 🔥 UPDATE: Visibility GONE krain taake naya data aate hi animation dobara chale
                recyclerView.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);

                silentRefresh();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    private void proTypewriter(TextView tv, String fullText) {
        Handler handler = new Handler();
        final int[] index = {0};

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index[0] <= fullText.length()) {
                    // Har character k sath cursor add kerna
                    String displayedText = fullText.substring(0, index[0]) + (index[0] % 2 == 0 ? "|" : " ");
                    tv.setText(displayedText);
                    index[0]++;
                    handler.postDelayed(this, 120); // Speed control
                } else {
                    // Typing khatam hone k baad cursor hata dena
                    tv.setText(fullText);
                }
            }
        }, 120);
    }
    private void setupRecyclerView() {
        adapter = new GalleryAdapter(Glide.with(this), viewModel,
                this::handlePreviewWithAd,
                (item, holder, uri, isVid, name, isDown) -> {
                    if (isDown) { SmartNotify.success(getView(), "Already Saved!"); return; }
                    if (isVid) adapter.handleAdThenDownload(item, holder, uri, name, true);
                    else checkPreferenceAndShowDialog(item, holder, uri, name);
                }
        );

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        // 🔥 UPDATE: Is line ko DefaultItemAnimator par set krain ya comment krain
        // recyclerView.setItemAnimator(null);

        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
    }

    private void handlePreviewWithAd(Object item) {
        AdManager.showInterstitial(getActivity(), new AdManager.AdCallback() {
            @Override
            public void onAdClosed() {
                openPreview(item);
            }

            @Override
            public void onAdFailed() {
                openPreview(item);
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(GalleryViewModel.class);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            swipeRefresh.setRefreshing(state.isRefreshing);

            boolean isDataCorrectType = false;
            if (state.data != null && !state.data.isEmpty()) {
                Object firstItem = state.data.get(0);
                if (isShowingVideo && firstItem instanceof VideoEntity) isDataCorrectType = true;
                else if (!isShowingVideo && firstItem instanceof ImageEntity) isDataCorrectType = true;
            }

            if (state.isLoading) {
                if (!isDataCorrectType || state.data == null || state.data.isEmpty()) {
                    progressBar.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    emptyLayout.setVisibility(View.GONE);
                    if (adapter != null) adapter.submitList(null);
                }
            } else {
                progressBar.setVisibility(View.GONE);

                if (state.showEmpty || !isDataCorrectType) {
                    if (state.showEmpty) {
                        recyclerView.setVisibility(View.GONE);
                        emptyLayout.setVisibility(View.VISIBLE);
                        setupEmptyText();
                    }
                } else {
                    emptyLayout.setVisibility(View.GONE);

                    // 🔥 UPDATE: Animation trigger logic
                    if (recyclerView.getVisibility() != View.VISIBLE) {
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.scheduleLayoutAnimation(); // XML wali animation chalay ga
                    }

                    adapter.submitList(state.data);
                }
            }
        });
    }
    private void setupEmptyText() {
        TextView tvTitle = emptyLayout.findViewById(R.id.tvEmptyTitle);
        TextView tvDesc = emptyLayout.findViewById(R.id.tvEmptyDescription);
        Button btnOpenWA = emptyLayout.findViewById(R.id.btnOpenWhatsApp);

        if (tvTitle != null && tvDesc != null) {
            tvTitle.setText(isShowingVideo ? "No Videos Found" : "No Images Found");
            tvDesc.setText("First watch status from WhatsApp\nthen download from here.");
        }

        if (btnOpenWA != null) {
            btnOpenWA.setOnClickListener(v -> {
                try {
                    Intent intent = getContext().getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                    if (intent != null) {
                        startContextActivity(intent);
                    } else {
                        Toast.makeText(getContext(), "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void startContextActivity(Intent intent) {
        if (isAdded() && getActivity() != null) {
            startActivity(intent);
        }
    }

    private void refreshData() { startLoadProcess(true); }
    private void silentRefresh() { startLoadProcess(false); }

    private void startLoadProcess(boolean isManual) {
        if (!isAdded()) return;

        // Agar manual (swipe) nahi hai, to spinner ko foran band krain
        if (!isManual && swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String uriStr = prefs.getString("statusFolderUri", null);
        if (uriStr != null) {
            viewModel.loadStatuses(requireContext(), Uri.parse(uriStr), isManual, isShowingVideo);
        }
    }

    private void openPreview(Object item) {
        Intent intent = new Intent(requireContext(), MediaPreviewActivity.class);

        String uri, name;
        boolean isDown, isVid;

        if (item instanceof ImageEntity) {
            ImageEntity img = (ImageEntity) item;
            uri = img.getUri();
            name = img.fileName;
            isDown = img.isDownloaded;
            isVid = false;
        } else {
            VideoEntity vid = (VideoEntity) item;
            uri = vid.getUri();
            name = vid.fileName;
            isDown = vid.isDownloaded;
            isVid = true;
        }

        intent.putExtra("EXTRA_URI", uri);
        intent.putExtra("EXTRA_IS_VIDEO", isVid);
        intent.putExtra("EXTRA_FILE_NAME", name);
        intent.putExtra("EXTRA_IS_DOWNLOADED", isDown);
        intent.putExtra("EXTRA_SOURCE", "MAGIC_LAB");

        startActivity(intent);
    }

    private void checkPreferenceAndShowDialog(Object item, GalleryAdapter.GalleryViewHolder holder, String uri, String name) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        if (prefs.getBoolean(KEY_ALWAYS_ORIGINAL, false)) {
            adapter.handleAdThenDownload(item, holder, uri, name, false);
        } else {
            showDownloadChoiceDialog(item, holder, uri, name);
        }
    }

    private void showDownloadChoiceDialog(Object item, GalleryAdapter.GalleryViewHolder holder, String uri, String name) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_download_choice, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        CheckBox cbAlwaysOriginal = dialogView.findViewById(R.id.cbDoNotShow);

        dialogView.findViewById(R.id.btnMagicLab).setOnClickListener(v -> {
            dialog.dismiss();
            handlePreviewWithAd(item);
        });

        dialogView.findViewById(R.id.btnOriginal).setOnClickListener(v -> {
            if (cbAlwaysOriginal.isChecked()) {
                requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_ALWAYS_ORIGINAL, true)
                        .apply();
            }

            dialog.dismiss();
            adapter.handleAdThenDownload(item, holder, uri, name, false);
        });

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        silentRefresh();
    }
}