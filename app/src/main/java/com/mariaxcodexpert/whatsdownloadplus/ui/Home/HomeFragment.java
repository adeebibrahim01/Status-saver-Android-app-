package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import android.view.*;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.stickers.StickerActivity;
import com.mariaxcodexpert.whatsdownloadplus.ui.support.SupportActivity;
import com.mariaxcodexpert.whatsdownloadplus.Helper.VersionHelper;
import com.mariaxcodexpert.whatsdownloadplus.databinding.FragmentHomeBinding;
import com.mariaxcodexpert.whatsdownloadplus.ui.Download.FullScreenMediaActivity;
import com.google.firebase.database.*;

import java.io.Serializable;
import java.util.*;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private RecentDownloadsAdapter adapter;
    private boolean isNavigating = false;
    private final Handler navHandler = new Handler(Looper.getMainLooper());
    private ValueEventListener supportListener;
    private DatabaseReference supportRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup cont, Bundle sav) {
        binding = FragmentHomeBinding.inflate(inf, cont, false);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);

        setupUI();
        observeState();

        checkSupportNotifications();
    }

    private void setupUI() {
        if (binding == null) return;

        GridLayoutManager glm = new GridLayoutManager(getContext(), 3);
        binding.homeRecentLayout.rvRecentDownloads.setLayoutManager(glm);
        binding.homeRecentLayout.rvRecentDownloads.setHasFixedSize(true);
        binding.homeRecentLayout.rvRecentDownloads.setItemAnimator(null);

        adapter = new RecentDownloadsAdapter(item -> safeAction(null, () -> {
            if (adapter == null || getActivity() == null) return;

            List<Object> currentList = adapter.getCurrentList();
            ArrayList<Serializable> serializableList = new ArrayList<>();
            for (Object obj : currentList) {
                if (obj instanceof Serializable) serializableList.add((Serializable) obj);
            }

            int pos = currentList.indexOf(item);

            AdManager.showInterstitial(getActivity(), new AdManager.AdCallback() {
                @Override
                public void onAdClosed() {
                    if (isAdded()) openMedia(serializableList, pos);
                }
                @Override
                public void onAdFailed() {
                    if (isAdded()) openMedia(serializableList, pos);
                }
            });
        }));

        binding.homeRecentLayout.rvRecentDownloads.setAdapter(adapter);

        binding.homeMenuLayout.btnImages.setOnClickListener(v -> safeAction(v, () -> navigateToGallery(false)));
        binding.homeMenuLayout.btnVideos.setOnClickListener(v -> safeAction(v, () -> navigateToGallery(true)));
        binding.homeMenuLayout.btnSaved.setOnClickListener(v -> safeAction(v, () -> navigate(R.id.action_home_to_downloads, null)));
        binding.homeMenuLayout.btnViral.setOnClickListener(v -> safeAction(v, () -> navigate(R.id.action_home_to_trending, null)));
        binding.homeMenuLayout.btnSearch.setOnClickListener(v -> safeAction(v, () -> navigate(R.id.nav_online_search, null)));

        binding.peekModeContainer.getRoot().setOnClickListener(v -> safeAction(v, () -> {
            Intent intent = new Intent(requireContext(), com.mariaxcodexpert.whatsdownloadplus.ui.peekmode.PeekModeActivity.class);
            startActivity(intent);
        }));
//        binding.tvTestTrending.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Activity ko call karne ke liye Intent use karein
//                android.content.Intent intent = new android.content.Intent(v.getContext(), StickerActivity.class);
//                v.getContext().startActivity(intent);
//            }
//        });
        binding.homeMenuLayout.btnReport.setOnClickListener(v -> safeAction(v, () -> {
            if (getContext() != null) {
                Intent intent = new Intent(requireContext(), SupportActivity.class);
                startActivity(intent);
            }
        }));

        try {
            String version = VersionHelper.getAppVersion(requireContext());
            binding.projectVersion.setText(version);
        } catch (Exception e) {
            binding.projectVersion.setText(getString(R.string.default_app_version));
        }
    }

    private void checkSupportNotifications() {
        if (getContext() == null) return;

        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String identifier;

            if (currentUser != null && currentUser.getUid() != null) {
                identifier = currentUser.getUid();
            } else {
                identifier = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            }

            supportRef = FirebaseDatabase.getInstance().getReference("Support").child("Tickets").child(identifier);

            supportListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (binding == null || !isAdded()) return;

                    boolean hasNewMsg = false;
                    if (snapshot.exists()) {
                        for (DataSnapshot ticket : snapshot.getChildren()) {
                            Boolean notify = ticket.child("hasNotification").getValue(Boolean.class);
                            if (notify != null && notify) {
                                hasNewMsg = true;
                                break;
                            }
                        }
                    }
                    binding.homeMenuLayout.redDotView.setVisibility(hasNewMsg ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Firebase_Home", "Notification check failed: " + error.getMessage());
                }
            };

            supportRef.orderByChild("hasNotification").equalTo(true).addValueEventListener(supportListener);

        } catch (Exception e) {
            Log.e("Firebase_Home", "Error setting up notifications", e);
        }
    }
    private void observeState() {
        viewModel.uiState.observe(getViewLifecycleOwner(), state -> {
            if (state == null || binding == null) return;

            if (state.stats != null) {
                binding.tvTodayCount.setVisibility(View.VISIBLE);
                binding.tvLast7DaysCount.setVisibility(View.VISIBLE);
                binding.tvActiveStreak.setVisibility(View.VISIBLE);

                binding.tvTodayCount.setText(String.valueOf(state.stats.todayCount));
                binding.tvLast7DaysCount.setText(String.valueOf(state.stats.totalCount));
                binding.tvActiveStreak.setText(String.valueOf(state.stats.activeStatuses));
                binding.joinedText.setText(state.stats.joinedDate);
            }

            List<Object> currentList = state.combinedList != null ? state.combinedList : new ArrayList<>();
            adapter.submitList(currentList);

            boolean empty = currentList.isEmpty();
            binding.homeRecentLayout.rvRecentDownloads.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.homeRecentLayout.tvRecentDownloadsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);

            toggleLoadingState(state.isSyncing, empty);
        });
    }

    private void toggleLoadingState(boolean syncing, boolean isListEmpty) {
        if (binding == null) return;
        int pVis = (syncing && isListEmpty) ? View.VISIBLE : View.GONE;
        binding.pbTodayCount.setVisibility(pVis);
        binding.pbLast7Days.setVisibility(pVis);
        binding.pbActiveStreak.setVisibility(pVis);

        float targetAlpha = (syncing) ? 0.6f : 1.0f;
        binding.tvTodayCount.setAlpha(targetAlpha);
        binding.tvLast7DaysCount.setAlpha(targetAlpha);
        binding.tvActiveStreak.setAlpha(targetAlpha);

        if (binding.tvTodayCount.getText().length() > 0) {
            binding.tvTodayCount.setVisibility(View.VISIBLE);
            binding.tvLast7DaysCount.setVisibility(View.VISIBLE);
            binding.tvActiveStreak.setVisibility(View.VISIBLE);
        }
    }

    private void safeAction(View v, Runnable act) {
        if (isNavigating) return;
        isNavigating = true;

        if (v != null) {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(60).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(60).withEndAction(act).start()).start();
        } else {
            act.run();
        }
        navHandler.postDelayed(() -> isNavigating = false, 400);
    }

    private void navigate(int id, Bundle b) {
        if (!isAdded() || binding == null) return;
        try {
            NavHostFragment.findNavController(this).navigate(id, b);
        } catch (Exception e) {
            Log.e("NAV_ERROR", "Navigation failed for ID: " + id);
        }
    }

    private void openMedia(ArrayList<Serializable> list, int pos) {
        Context ctx = getContext();
        if (ctx == null || list == null || list.isEmpty()) return;

        try {
            Intent intent = new Intent(ctx, FullScreenMediaActivity.class);
            intent.putExtra("EXTRA_MEDIA_LIST", list);
            intent.putExtra("EXTRA_POSITION", pos);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("OPEN_MEDIA", "Error starting FullScreenMedia: " + e.getMessage());
        }
    }
    private void navigateToGallery(boolean vid) { Bundle b = new Bundle(); b.putBoolean("arg_is_video", vid); navigate(R.id.nav_gallery, b); }



    @Override
    public void onResume() {
        super.onResume();
        isNavigating = false;
        navHandler.postDelayed(() -> {
            if (viewModel != null && binding != null && !isNavigating) {
                viewModel.refreshDashboardData();
            }
        }, 1200);
    }

    @Override
    public void onDestroyView() {
        navHandler.removeCallbacksAndMessages(null);
        if (supportRef != null && supportListener != null) {
            supportRef.removeEventListener(supportListener);
        }
        binding = null;

        super.onDestroyView();
    }
}