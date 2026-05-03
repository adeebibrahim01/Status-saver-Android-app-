package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.SmartNotify;
import com.mariaxcodexpert.whatsdownloadplus.ui.base.BaseMediaFragment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DownloadFragment extends BaseMediaFragment {

    private MediaListAdapter adapter;
    private DownloadViewModel vm;
    private RecyclerView recyclerView;
    private View tvEmptyMessage;
    private ProgressBar progressBar;

    private ActivityResultLauncher<IntentSenderRequest> deleteLauncher;
    private ActivityResultLauncher<Intent> fullScreenLauncher;

    private boolean isInitialAnimationDone = false;
    private ObjectAnimator pulseAnimatorX;
    private ObjectAnimator pulseAnimatorY;
    private TextView Headertext ;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deleteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (vm != null) {
                            vm.completePendingDelete();
                            vm.refreshSavedFiles();
                        }
                    }
                }
        );

        fullScreenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == FullScreenMediaActivity.RESULT_DELETED || result.getResultCode() == Activity.RESULT_OK) {
                        if (vm != null) vm.refreshSavedFiles();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_download, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        recyclerView = v.findViewById(R.id.recyclerView);
        tvEmptyMessage = v.findViewById(R.id.tvEmptyMessage);
        progressBar = v.findViewById(R.id.firstLoadProgress);
        Headertext =v.findViewById(R.id.toolbarSubtitle);

       // proTypewriter(Headertext, "Status Downloads");

        vm = new ViewModelProvider(this).get(DownloadViewModel.class);

        setupAdapterAndRV(v);
        observeViewModel();

        vm.refreshSavedFiles();
    }

    private void setupAdapterAndRV(View v) {
        adapter = new MediaListAdapter(item -> {
            performHaptic();
            AdManager.showInterstitial(getActivity(), new AdManager.AdCallback() {
                @Override
                public void onAdClosed() { if (vm != null) vm.deleteFile(item); }
                @Override public void onAdFailed() { if (vm != null) vm.deleteFile(item); }
            });
        });

        adapter.setOnItemClickListener(item -> {
            if (!isFragmentValid() || recyclerView == null) return;

            List<Object> currentList = adapter.getCurrentList();
            int position = currentList.indexOf(item);

            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
            if (holder instanceof MediaListAdapter.VH) {
                MediaListAdapter.VH vh = (MediaListAdapter.VH) holder;
                if (vh.deleteOverlay != null && vh.deleteOverlay.getVisibility() == View.VISIBLE) return;
            }

            AdManager.showInterstitial(getActivity(), new AdManager.AdCallback() {
                @Override
                public void onAdClosed() { launchFullScreen(currentList, position); }
                @Override public void onAdFailed() { launchFullScreen(currentList, position); }
            });
        });

        // 1. Base setup call krain
        setupMediaRecyclerView(recyclerView, adapter, 3);

        // 2. 🔥 Layout Animation ko manually load aur attach krain
        // Is se confirm ho jayega ke XML wali animation controller activate ho gayi hai
        android.view.animation.LayoutAnimationController controller =
                android.view.animation.AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down);

        recyclerView.setLayoutAnimation(controller);

        // 3. 🔥 Item Animator ko default par rakhein ya reset krain
        // Agar Base class ne isay null kiya hai, to waterfall effect "blink" ban jayega
        if (recyclerView.getItemAnimator() == null) {
            recyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
        }
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
    private void launchFullScreen(List<Object> currentList, int position) {
        Intent intent = new Intent(getContext(), FullScreenMediaActivity.class);
        ArrayList<Serializable> serializableList = new ArrayList<>();
        for (Object obj : currentList) {
            if (obj instanceof Serializable) serializableList.add((Serializable) obj);
        }

        intent.putExtra(MediaListAdapter.EXTRA_MEDIA_LIST, serializableList);
        intent.putExtra(MediaListAdapter.EXTRA_POSITION, position);
        fullScreenLauncher.launch(intent);
    }

    private void observeViewModel() {
        vm.uiState.observe(getViewLifecycleOwner(), state -> {
            if (!isFragmentValid() || state == null) return;

            // Progress bar handling
            if (progressBar != null) {
                progressBar.setVisibility(state.isLoading && (state.data == null || state.data.isEmpty()) ? View.VISIBLE : View.GONE);
            }

            boolean hasData = state.data != null && !state.data.isEmpty();

            if (hasData) {
                stopEmptyAnimation();
                tvEmptyMessage.setVisibility(View.GONE);

                // 1. Pehle data submit kero taake RecyclerView ko pata ho ke items aa gaye hain
                adapter.submit(state.data, tvEmptyMessage, recyclerView);

                // 2. Phir check kero ke kya animation dikhani hai
                if (recyclerView.getVisibility() != View.VISIBLE || !isInitialAnimationDone) {
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerView.setAlpha(1.0f);

                    // 🔥 Waterfall effect ko yahan trigger krain
                    recyclerView.scheduleLayoutAnimation();
                    isInitialAnimationDone = true;
                }

            } else {
                // Loading khatam hone par agar data nahi hai to empty show krain
                if (!state.isLoading) {
                    adapter.submit(new ArrayList<>(), tvEmptyMessage, recyclerView);
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyMessage.setVisibility(View.VISIBLE);
                    updateEmptyStateContent();

                    // Reset krain taake jab next time data aaye to phir se waterfall chale
                    isInitialAnimationDone = false;
                }
            }
        });

        vm.permissionIntent.observe(getViewLifecycleOwner(), pendingIntent -> {
            if (pendingIntent != null) {
                IntentSenderRequest request = new IntentSenderRequest.Builder(pendingIntent).build();
                deleteLauncher.launch(request);
                vm.clearPermissionIntent();
            }
        });
    }

    private void updateEmptyStateContent() {
        if (tvEmptyMessage == null) return;

        TextView title = tvEmptyMessage.findViewById(R.id.tvEmptyTitle);
        TextView desc = tvEmptyMessage.findViewById(R.id.tvEmptyDescription);
        ImageView icon = tvEmptyMessage.findViewById(R.id.ivEmptyIcon);

        if (title != null && desc != null) {
            title.setText("No Saved Media");
            desc.setText("Save your favorite statuses to view them here anytime.");

            if (icon != null && pulseAnimatorX == null) {
                pulseAnimatorX = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.1f);
                pulseAnimatorY = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.1f);
                pulseAnimatorX.setDuration(1000);
                pulseAnimatorY.setDuration(1000);
                pulseAnimatorX.setRepeatMode(ValueAnimator.REVERSE);
                pulseAnimatorY.setRepeatMode(ValueAnimator.REVERSE);
                pulseAnimatorX.setRepeatCount(ValueAnimator.INFINITE);
                pulseAnimatorY.setRepeatCount(ValueAnimator.INFINITE);
                pulseAnimatorX.start();
                pulseAnimatorY.start();
            }
        }
    }

    private void stopEmptyAnimation() {
        if (pulseAnimatorX != null) {
            pulseAnimatorX.cancel();
            pulseAnimatorY.cancel();
            pulseAnimatorX = null;
            pulseAnimatorY = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) vm.refreshSavedFiles();
    }

    @Override
    public void onDestroyView() {
        stopEmptyAnimation();
        super.onDestroyView();
    }
}