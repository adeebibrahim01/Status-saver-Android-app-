package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.base.BaseMediaFragment;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DownloadFragment extends BaseMediaFragment {

    private MediaListAdapter adapter;
    private DownloadViewModel vm;
    private RecyclerView rv;
    private View emptyView;
    private ProgressBar pb;
    private ActivityResultLauncher<IntentSenderRequest> deleteLauncher;
    private boolean isDataInitialized = false;

    @Override
    public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        deleteLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), r -> {
            if (r.getResultCode() == Activity.RESULT_OK && vm != null) {
                vm.completePendingDelete();
                vm.refreshSavedFiles();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_download, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        rv = v.findViewById(R.id.recyclerView);
        emptyView = v.findViewById(R.id.tvEmptyMessage);
        pb = v.findViewById(R.id.firstLoadProgress);
        vm = new ViewModelProvider(this).get(DownloadViewModel.class);


        if (rv != null) {
            rv.setHasFixedSize(true);
            rv.setItemViewCacheSize(20);
            rv.setDrawingCacheEnabled(true);
            rv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
            rv.setItemAnimator(null);
        }

        setupAdapter();
        observeViewModel();
        vm.refreshSavedFiles();
    }
    private void showStatus(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAdapter() {
        adapter = new MediaListAdapter(item -> {
            Activity activity = getActivity();
            if (activity == null || isDetached()) return;

            AdManager.showInterstitial(activity, new AdManager.AdCallback() {
                @Override public void onAdClosed() { executeDelete(item); }
                @Override public void onAdFailed() { executeDelete(item); }
            });
        });

        adapter.setOnItemClickListener(item -> {
            if (!isFragmentValid() || rv == null) return;

            int pos = adapter.getCurrentList().indexOf(item);
            if (pos == -1) {
                showStatus(getString(R.string.error_item_unavailable));
                return;
            }

            RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(pos);
            if (vh instanceof MediaListAdapter.VH && ((MediaListAdapter.VH) vh).deleteOverlay.getVisibility() == View.VISIBLE) return;

            Activity activity = getActivity();
            if (activity == null) return;

            AdManager.showInterstitial(activity, new AdManager.AdCallback() {
                @Override public void onAdClosed() { launchFullScreen(adapter.getCurrentList(), pos); }
                @Override public void onAdFailed() { launchFullScreen(adapter.getCurrentList(), pos); }
            });
        });

        setupMediaRecyclerView(rv, adapter, 3);
    }

    private void executeDelete(Object item) {
        if (vm != null) {
            try {
                vm.deleteFile(item);
            } catch (Exception e) {
                showStatus(getString(R.string.error_process_interrupted));
                e.printStackTrace();
            }
        }
    }

    private void launchFullScreen(List<Object> list, int pos) {
        try {
            if (getContext() == null || list == null || pos < 0) return;

            ArrayList<Serializable> sList = new ArrayList<>();
            for (Object obj : list) {
                if (obj instanceof Serializable) {
                    sList.add((Serializable) obj);
                }
            }

            if (sList.isEmpty()) {
                showStatus(getString(R.string.error_media_not_found));
                return;
            }

            Intent intent = new Intent(getContext(), FullScreenMediaActivity.class);
            intent.putExtra(MediaListAdapter.EXTRA_MEDIA_LIST, sList);
            intent.putExtra(MediaListAdapter.EXTRA_POSITION, pos);
            startActivity(intent);

        } catch (Exception e) {
            showStatus(getString(R.string.error_could_not_open_media));
            e.printStackTrace();
        }
    }

    private void observeViewModel() {
        vm.uiState.observe(getViewLifecycleOwner(), state -> {
            if (!isFragmentValid() || state == null) return;

            boolean hasData = state.data != null && !state.data.isEmpty();

            if (pb != null) {
                if (state.isLoading && !isDataInitialized) {
                    pb.setVisibility(View.VISIBLE);
                } else {
                    pb.setVisibility(View.GONE);
                }
            }

            if (!state.isLoading || hasData) {
                isDataInitialized = true;
                if (hasData) {
                    if (emptyView != null) emptyView.setVisibility(View.GONE);
                    if (rv != null) rv.setVisibility(View.VISIBLE);
                    adapter.submit(state.data, emptyView, rv);
                } else {
                    if (rv != null) rv.setVisibility(View.GONE);
                    if (emptyView != null) {
                        emptyView.setVisibility(View.VISIBLE);
                        updateEmptyState();
                    }
                    adapter.submit(new ArrayList<>(), emptyView, rv);
                }
            }
        });

        vm.permissionIntent.observe(getViewLifecycleOwner(), pi -> {
            if (pi != null) {
                try {
                    deleteLauncher.launch(new IntentSenderRequest.Builder(pi).build());
                } catch (Exception e) {
                    showStatus(getString(R.string.error_system_permission_failed));
                    e.printStackTrace();
                }
                vm.clearPermissionIntent();
            }
        });
    }
    private void updateEmptyState() {
        if (emptyView == null) return;
        ((TextView) emptyView.findViewById(R.id.tvEmptyTitle)).setText(getString(R.string.empty_saved_media_title));
        ((TextView) emptyView.findViewById(R.id.tvEmptyDescription)).setText(getString(R.string.empty_saved_media_desc));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) vm.refreshSavedFiles();
    }
}