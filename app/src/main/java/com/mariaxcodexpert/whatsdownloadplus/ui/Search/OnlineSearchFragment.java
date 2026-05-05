package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.MediaStatusUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OnlineSearchFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView recyclerView;
    private ProgressBar loader;
    private View tvNoStatus;
    private OnlineImageAdapter adapter;
    private List<MediaItem> mediaList = new ArrayList<>();
    private OnlineMediaViewModel viewModel;
    private TextView toolbarTitle, toolbarSubtitle;
    private UserPsychologyManager psychologyManager;

    private final String API_KEY = com.mariaxcodexpert.whatsdownloadplus.BuildConfig.API_KEY;
    private int currentPage = 1;
    private boolean isLoading = false;
    private String currentQuery = "";

    private SharedPreferences prefs;
    private static final String PREF_NAME = "UserInterests";

    private FirebaseAnalytics mFirebaseAnalytics;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_online_search, container, false);

        etSearch = root.findViewById(R.id.etSearch);
        recyclerView = root.findViewById(R.id.rvOnlineImages);
        loader = root.findViewById(R.id.loader);
        tvNoStatus = root.findViewById(R.id.tvNoStatus);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext());
        if (getActivity() != null) {
            toolbarTitle = getActivity().findViewById(R.id.toolbarTitle);
            toolbarSubtitle = getActivity().findViewById(R.id.toolbarSubtitle);
        }

        prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        psychologyManager = new UserPsychologyManager(requireContext());

        // ViewModel Initialization
        viewModel = new ViewModelProvider(requireActivity()).get(OnlineMediaViewModel.class);

        setupRecyclerView();

        // Observe ViewModel Data
        viewModel.getMediaList().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                mediaList.clear();
                mediaList.addAll(items);
                if (adapter != null) adapter.notifyDataSetChanged();

                // Visibility handle kerna
                if (!mediaList.isEmpty()) {
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerView.setAlpha(1.0f);
                    tvNoStatus.setVisibility(View.GONE);
                }
            }
        });

        // Restore State or Init
        if (viewModel.getLastQuery().isEmpty()) {
            initDiscovery();
        } else {
            currentQuery = viewModel.getLastQuery();
            currentPage = viewModel.getLastPage();
            updateToolbarUI(currentQuery);
        }

        root.findViewById(R.id.btnSearch).setOnClickListener(v -> performSearch());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        return root;
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);

        android.view.animation.LayoutAnimationController controller =
                android.view.animation.AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fall_down);
        recyclerView.setLayoutAnimation(controller);

        adapter = new OnlineImageAdapter(getContext(), mediaList);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new OnlineImageAdapter.OnItemClickListener() {
            @Override
            public void onDownloadClick(MediaItem item) {
                int position = mediaList.indexOf(item);
                showAd(() -> handleMediaDownload(item, position));
            }

            @Override
            public void onPreviewClick(MediaItem item) {
                showAd(() -> {
                    Intent intent = new Intent(getContext(), OnlineMediaPreviewActivity.class);
                    intent.putExtra("MEDIA_LIST", (java.io.Serializable) new ArrayList<>(mediaList));
                    intent.putExtra("POSITION", mediaList.indexOf(item));
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                });
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && !isLoading) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 6) {
                        isLoading = true;
                        currentPage++;
                        viewModel.setLastPage(currentPage); // Sync with ViewModel
                        fetchMixedContent(currentQuery, false);
                    }
                }
            }
        });
    }

    private void handleMediaDownload(MediaItem item, int position) {
        if (item.isDownloaded()) {
            Toast.makeText(getContext(), "Already Saved ✅", Toast.LENGTH_SHORT).show();
            return;
        }

        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
        if (holder != null) {
            holder.itemView.findViewById(R.id.downloadOverlay).setVisibility(View.VISIBLE);
            ((TextView) holder.itemView.findViewById(R.id.progressText)).setText("0%");
        }

        MediaStatusUtils.executor.execute(() -> {
            try {
                String downloadUrl = item.isVideo() ? item.getVideoUrl() : item.getUrl();
                String fileName = "Pexels_" + Math.abs(downloadUrl.hashCode()) + (item.isVideo() ? ".mp4" : ".jpg");

                for (int p = 15; p <= 85; p += 25) {
                    final int progress = p;
                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            if (holder != null) {
                                ((TextView) holder.itemView.findViewById(R.id.progressText)).setText(progress + "%");
                            }
                        });
                    }
                    Thread.sleep(150);
                }

                File file = Glide.with(requireContext())
                        .asFile()
                        .load(downloadUrl)
                        .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get();

                if (file != null && file.exists()) {
                    MediaStatusUtils.saveToGallery(requireContext(), Uri.fromFile(file), null, fileName, item.isVideo(), 100, (success, savedUri) -> {
                        if (getActivity() != null && isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                if (holder != null) holder.itemView.findViewById(R.id.downloadOverlay).setVisibility(View.GONE);
                                if (success) {
                                    viewModel.updateDownloadStatus(downloadUrl, true); // Update via ViewModel
                                    Toast.makeText(getContext(), "Saved Successfully! ✧", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (holder != null) holder.itemView.findViewById(R.id.downloadOverlay).setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Download Failed", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager)
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showAd(Runnable afterAdAction) {
        if (getActivity() != null && AdManager.isInterstitialLoaded()) {
            AdManager.showInterstitial(getActivity(), new AdManager.AdCallback() {
                @Override public void onAdClosed() { afterAdAction.run(); }
                @Override public void onAdFailed() { afterAdAction.run(); }
            });
        } else {
            afterAdAction.run();
        }
    }

    private void fetchMixedContent(String query, boolean isNewSearch) {
        if (!isNetworkAvailable()) {
            showNoInternetUI();
            return;
        }

        if (isNewSearch && isAdded()) {
            loader.setVisibility(View.VISIBLE);
            tvNoStatus.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        }

        final String encodedQuery = Uri.encode(query.trim().isEmpty() ? "trending" : query.trim());
        OkHttpClient client = new OkHttpClient();

        // Photos API
        Request photoReq = new Request.Builder()
                .url("https://api.pexels.com/v1/search?query=" + encodedQuery + "&per_page=30&page=" + currentPage + "&orientation=portrait")
                .addHeader("Authorization", API_KEY)
                .build();

        client.newCall(photoReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { handleApiError(isNewSearch); }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                List<MediaItem> photosList = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray photos = json.optJSONArray("photos");
                        if (photos != null) {
                            for (int i = 0; i < photos.length(); i++) {
                                String imgUrl = photos.getJSONObject(i).getJSONObject("src").optString("large2x");
                                if (imgUrl != null) photosList.add(new MediaItem(imgUrl, null, false));
                            }
                        }
                    } catch (Exception ignored) {}
                }
                updateUI(photosList, isNewSearch);
            }
        });

        // Videos API
        Request videoReq = new Request.Builder()
                .url("https://api.pexels.com/videos/search?query=" + encodedQuery + "&per_page=15&page=" + currentPage + "&orientation=portrait")
                .addHeader("Authorization", API_KEY)
                .build();

        client.newCall(videoReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { checkAndHideLoader(isNewSearch); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                List<MediaItem> videosList = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray videos = json.optJSONArray("videos");
                        if (videos != null) {
                            for (int i = 0; i < videos.length(); i++) {
                                JSONObject vObj = videos.getJSONObject(i);
                                String thumb = vObj.optString("image");
                                JSONArray files = vObj.optJSONArray("video_files");
                                if (files != null && files.length() > 0) {
                                    videosList.add(new MediaItem(thumb, files.getJSONObject(0).optString("link"), true));
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                updateUI(videosList, false);
            }
        });
    }

    private void handleApiError(boolean isNewSearch) {
        if (isNewSearch && isAdded()) {
            getActivity().runOnUiThread(() -> {
                loader.setVisibility(View.GONE);
                if (mediaList.isEmpty()) tvNoStatus.setVisibility(View.VISIBLE);
            });
        }
    }

    private void showNoInternetUI() {
        if (!isAdded()) return;
        getActivity().runOnUiThread(() -> {
            loader.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            tvNoStatus.setVisibility(View.VISIBLE);
        });
    }

    private void checkAndHideLoader(boolean isNewSearch) {
        if (isNewSearch && isAdded()) getActivity().runOnUiThread(() -> loader.setVisibility(View.GONE));
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (!query.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, query);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle);

            psychologyManager.trackSearch(query);
            currentQuery = query;
            currentPage = 1;
            isLoading = true;

            // ViewModel state update
            viewModel.setLastQuery(currentQuery);
            viewModel.clearList();

            if (recyclerView != null) {
                recyclerView.setVisibility(View.INVISIBLE);
                recyclerView.setAlpha(0f);
            }

            updateToolbarUI(query);
            if (loader != null) loader.setVisibility(View.VISIBLE);
            if (tvNoStatus != null) tvNoStatus.setVisibility(View.GONE);

            fetchMixedContent(query, true);
            etSearch.clearFocus();
            hideKeyboard();
        }
    }

    private void hideKeyboard() {
        View view = getActivity() != null ? getActivity().getCurrentFocus() : null;
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void updateUI(List<MediaItem> newItems, boolean isNewSearch) {
        if (!isAdded() || newItems == null) return;
        MediaStatusUtils.executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            for (MediaItem item : newItems) {
                String url = item.isVideo() ? item.getVideoUrl() : item.getUrl();
                String fileName = "Pexels_" + Math.abs(url.hashCode()) + (item.isVideo() ? ".mp4" : ".jpg");
                if (db.imageDao().isImageExists(fileName) || db.videoDao().isVideoExists(fileName)) {
                    item.setDownloaded(true);
                }
            }

            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                    if (loader != null) loader.setVisibility(View.GONE);

                    if (isNewSearch) {
                        Collections.shuffle(newItems);
                        viewModel.setMediaList(newItems); // New search triggers reset
                        if (!newItems.isEmpty()) {
                            recyclerView.scheduleLayoutAnimation();
                        }
                    } else {
                        viewModel.addMediaItems(newItems); // Pagination triggers append
                    }

                    isLoading = false;
                });
            }
        });
    }

    private void initDiscovery() {
        String aiQuery = psychologyManager.getAIPredictedQuery();

        if (aiQuery == null || aiQuery.trim().isEmpty()) {
            // 🔥 Strings.xml se array load karne ka tarika
            String[] tags = getResources().getStringArray(R.array.trending_tags);
            aiQuery = tags[new Random().nextInt(tags.length)];
        }

        currentQuery = aiQuery;
        viewModel.setLastQuery(currentQuery);
        updateToolbarUI(currentQuery);
        fetchMixedContent(currentQuery, true);
    }

    private void updateToolbarUI(String query) {
        if (toolbarTitle != null) toolbarTitle.setText("✧ " + query.toUpperCase() + " ✧");
        if (toolbarSubtitle != null) toolbarSubtitle.setText("Premium Discovery");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Database sync check (Optional but good for data consistency)
        if (!mediaList.isEmpty()) {
            MediaStatusUtils.executor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(getContext());
                boolean changed = false;
                for (MediaItem item : mediaList) {
                    if (!item.isDownloaded()) {
                        String url = item.isVideo() ? item.getVideoUrl() : item.getUrl();
                        String fileName = "Pexels_" + Math.abs(url.hashCode()) + (item.isVideo() ? ".mp4" : ".jpg");
                        if (db.imageDao().isImageExists(fileName) || db.videoDao().isVideoExists(fileName)) {
                            item.setDownloaded(true);
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    getActivity().runOnUiThread(() -> viewModel.setMediaList(new ArrayList<>(mediaList)));
                }
            });
        }
    }
}