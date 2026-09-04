package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
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
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle, tvEmptyDescription;
    private MaterialButton btnRetrySearch;

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
        ImageView btnClearSearch = root.findViewById(R.id.btnClearSearch);
        ImageView btnSearchIcon = root.findViewById(R.id.btnSearch);
        recyclerView = root.findViewById(R.id.rvOnlineImages);
        loader = root.findViewById(R.id.loader);
        tvNoStatus = root.findViewById(R.id.tvNoStatus);
        ivEmptyIcon = root.findViewById(R.id.ivEmptyIcon);
        tvEmptyTitle = root.findViewById(R.id.tvEmptyTitle);
        tvEmptyDescription = root.findViewById(R.id.tvEmptyDescription);
        btnRetrySearch = root.findViewById(R.id.btnRetrySearch);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext());
        if (getActivity() != null) {
            toolbarTitle = getActivity().findViewById(R.id.toolbarTitle);
            toolbarSubtitle = getActivity().findViewById(R.id.toolbarSubtitle);
        }
        prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        psychologyManager = new UserPsychologyManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(OnlineMediaViewModel.class);

        setupRecyclerView();

        viewModel.getMediaList().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                mediaList.clear();
                mediaList.addAll(items);
                if (adapter != null) adapter.notifyDataSetChanged();

                if (!mediaList.isEmpty()) {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvNoStatus.setVisibility(View.GONE);
                    loader.setVisibility(View.GONE);
                }
            }
        });

        List<MediaItem> currentList = viewModel.getMediaList().getValue();
        if (currentList == null || currentList.isEmpty()) {
            initDiscovery();
        } else {
            currentQuery = viewModel.getLastQuery();
            updateToolbarUI(currentQuery);
            recyclerView.setVisibility(View.VISIBLE);
            tvNoStatus.setVisibility(View.GONE);
        }
        if (btnSearchIcon != null) btnSearchIcon.setOnClickListener(v -> performSearch());

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                etSearch.setText("");
                btnClearSearch.setVisibility(View.GONE);
            });
        }

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        btnRetrySearch.setOnClickListener(v -> performSearch());

        return root;
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        adapter = new OnlineImageAdapter(getContext(), mediaList);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new OnlineImageAdapter.OnItemClickListener() {
            @Override
            public void onDownloadClick(MediaItem item) {
                showAd(() -> handleMediaDownload(item, mediaList.indexOf(item)));
            }

            @Override
            public void onPreviewClick(MediaItem item) {
                showAd(() -> {
                    Intent intent = new Intent(getContext(), OnlineMediaPreviewActivity.class);
                    intent.putExtra("MEDIA_LIST", (java.io.Serializable) new ArrayList<>(mediaList));
                    intent.putExtra("POSITION", mediaList.indexOf(item));
                    startActivity(intent);
                });
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && !isLoading) {
                    if (layoutManager.findLastVisibleItemPosition() >= layoutManager.getItemCount() - 6) {
                        isLoading = true;
                        currentPage++;
                        viewModel.setLastPage(currentPage);
                        fetchMixedContent(currentQuery, false);
                    }
                }
            }
        });
    }

    private void handleMediaDownload(MediaItem item, int position) {
        if (item.isDownloaded()) return;

        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
        View overlay = (holder != null) ? holder.itemView.findViewById(R.id.downloadOverlay) : null;
        TextView progressTv = (holder != null) ? holder.itemView.findViewById(R.id.progressText) : null;

        com.google.android.material.progressindicator.CircularProgressIndicator progressBar =
                (holder != null) ? holder.itemView.findViewById(R.id.neonProgressBar) : null;

        if (overlay != null) {
            overlay.setVisibility(View.VISIBLE);
            if (progressTv != null) progressTv.setText(getString(R.string.progress_initial_percent));
            if (progressBar != null) {
                progressBar.setIndeterminate(false);
                progressBar.setMax(100);
                progressBar.setProgress(0);
            }
        }

        String downloadUrl = item.isVideo() ? item.getVideoUrl() : item.getUrl();
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            if (overlay != null) overlay.setVisibility(View.GONE);
            return;
        }

        String fileName = "Pexels_" + Math.abs(downloadUrl.hashCode()) + (item.isVideo() ? ".mp4" : ".jpg");
        Context appContext = requireActivity().getApplicationContext();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handleDownloadError(holder, e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    handleDownloadError(holder, "Server error");
                    return;
                }

                long totalBytes = response.body().contentLength();
                File tempFile = new File(appContext.getCacheDir(), fileName);

                try (okio.BufferedSink sink = okio.Okio.buffer(okio.Okio.sink(tempFile));
                     okio.BufferedSource source = response.body().source()) {

                    long bytesRead = 0;
                    long read;
                    while ((read = source.read(sink.buffer(), 8192)) != -1) {
                        bytesRead += read;
                        final int progress = totalBytes > 0 ? (int) ((bytesRead * 100) / totalBytes) : 0;

                        if (getActivity() != null && isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                if (progressTv != null) progressTv.setText(progress + "%");
                                // 🔥 Ab neonProgressBar fill hoga
                                if (progressBar != null) progressBar.setProgress(progress);
                            });
                        }
                    }
                    sink.writeAll(source);
                    sink.flush();

                    MediaStatusUtils.saveToGallery(appContext, Uri.fromFile(tempFile), null, fileName, item.isVideo(), 100, (success, savedUri) -> {
                        if (getActivity() != null && isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                if (overlay != null) overlay.setVisibility(View.GONE);
                                if (success) {
                                    viewModel.updateDownloadStatus(downloadUrl, true);
                                }
                            });
                        }
                    });

                } catch (Exception e) {
                    handleDownloadError(holder, e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }
    private void handleDownloadError(RecyclerView.ViewHolder holder, String debugMsg) {
        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> {
                if (holder != null) {
                    holder.itemView.findViewById(R.id.downloadOverlay).setVisibility(View.GONE);
                }
                String errorMsg;
                if (!isNetworkAvailable()) {
                    errorMsg = getString(R.string.error_no_internet);
                         }
                else if (debugMsg != null && (debugMsg.contains("timeout") || debugMsg.contains("Unable to resolve host"))) {
                    errorMsg = getString(R.string.error_internet_unstable);
                }
                else {
                    errorMsg = getString(R.string.error_unable_to_download);
                }

                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                Log.e("SearchDebug", "Download Error: " + debugMsg);
            });
        }
    }

    private void fetchMixedContent(String query, boolean isNewSearch) {
        if (!isAdded() || getContext() == null) return;

        if (!isNetworkAvailable()) {
            showEmptyState(true,
                    getString(R.string.empty_title_no_connection),
                    getString(R.string.empty_desc_no_connection),
                    R.drawable.ic_wifi_off,
                    true);
            return;
        }

        if (isNewSearch) {
            showEmptyState(false, "", "", 0, false);
            loader.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        final String encodedQuery = Uri.encode(query.trim().isEmpty() ? "trending" : query.trim());
        OkHttpClient client = new OkHttpClient();

        Request photoReq = new Request.Builder()
                .url("https://api.pexels.com/v1/search?query=" + encodedQuery + "&per_page=30&page=" + currentPage + "&orientation=portrait")
                .addHeader("Authorization", API_KEY).build();

        client.newCall(photoReq).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> handleApiError(isNewSearch));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);
                        JSONArray photos = json.optJSONArray("photos");
                        List<MediaItem> rawList = new ArrayList<>();

                        if (photos != null) {
                            for (int i = 0; i < photos.length(); i++) {
                                JSONObject obj = photos.getJSONObject(i);
                                rawList.add(new MediaItem(
                                        obj.getJSONObject("src").optString("large2x"),
                                        null,
                                        false,
                                        obj.optString("alt", "Premium Status")
                                ));
                            }
                        }
                 if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> updateUI(rawList, isNewSearch));
                        }
                    } else {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> handleApiError(isNewSearch));
                        }
                    }
                } catch (Exception e) {
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> updateUI(new ArrayList<>(), isNewSearch));
                    }
                }
            }
        });
    }
    private void showEmptyState(boolean show, String title, String desc, int iconRes, boolean showRetry) {
        if (!isAdded()) return;
        getActivity().runOnUiThread(() -> {
            if (show) {
                recyclerView.setVisibility(View.GONE);
                loader.setVisibility(View.GONE);
                tvNoStatus.setVisibility(View.VISIBLE);
                tvEmptyTitle.setText(title);
                tvEmptyDescription.setText(desc);
                if (iconRes != 0) ivEmptyIcon.setImageResource(iconRes);
                btnRetrySearch.setVisibility(showRetry ? View.VISIBLE : View.GONE);
            } else {
                tvNoStatus.setVisibility(View.GONE);
            }
        });
    }

    private void handleApiError(boolean isNewSearch) {
        if (isNewSearch) {
            showEmptyState(true, getString(R.string.empty_title_server_error), getString(R.string.empty_desc_server_error), R.drawable.ic_error_api, true);
        } else {
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), getString(R.string.toast_failed_load_more), Toast.LENGTH_SHORT).show());
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (!query.isEmpty()) {
            currentQuery = query;
            currentPage = 1;
            isLoading = true;
            viewModel.setLastQuery(currentQuery);
            viewModel.clearList();
            updateToolbarUI(query);
            fetchMixedContent(query, true);
            hideKeyboard();
        }
    }

    private void updateUI(List<MediaItem> newItems, boolean isNewSearch) {
        if (!isAdded()) return;
        if (isNewSearch && (newItems == null || newItems.isEmpty())) {
            showEmptyState(true, getString(R.string.empty_title_no_results), getString(R.string.empty_desc_no_results), R.drawable.ic_error_api, false);
            return;
        }

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
                    loader.setVisibility(View.GONE);
                    if (isNewSearch) {
                        Collections.shuffle(newItems);
                        viewModel.setMediaList(newItems);
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.scheduleLayoutAnimation();
                    } else {
                        viewModel.addMediaItems(newItems);
                    }
                    isLoading = false;
                });
            }
        });
    }
    private void initDiscovery() {
        String aiQuery = psychologyManager.getAIPredictedQuery();
        if (aiQuery == null || aiQuery.trim().isEmpty()) {
            aiQuery = "Luxury Car";
        }
        proceedWithDiscovery(aiQuery);
    }
    private void proceedWithDiscovery(String query) {
        if (!isAdded()) return;

        getActivity().runOnUiThread(() -> {
            currentQuery = query;
            viewModel.setLastQuery(currentQuery);
            updateToolbarUI(currentQuery);
            fetchMixedContent(currentQuery, true);
        });
    }

    private void showAd(Runnable action) {
        if (getActivity() != null && AdManager.isInterstitialLoaded()) {
            AdManager.showInterstitial(getActivity(), new AdManager.AdCallback() {
                @Override public void onAdClosed() { action.run(); }
                @Override public void onAdFailed() { action.run(); }
            });
        } else action.run();
    }

    private void updateToolbarUI(String query) {
        if (toolbarTitle != null) {
            // Query null na ho iska dhyan rakhein
            String displayQuery = (query != null && !query.isEmpty()) ? query : "Luxury Car";
            toolbarTitle.setText("✧ " + displayQuery.toUpperCase() + " ✧");
        }
        if (toolbarSubtitle != null) {
            toolbarSubtitle.setText(getString(R.string.premium_discovery_subtitle));
        }
    }

    private void hideKeyboard() {
        View view = getActivity() != null ? getActivity().getCurrentFocus() : null;
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    @Override
    public void onResume() {
        super.onResume();

        if (!isNetworkAvailable()) {
            showEmptyState(true,
                    getString(R.string.empty_title_no_connection),
                    getString(R.string.empty_desc_no_connection),
                    R.drawable.ic_wifi_off,
                    true);
        } else {
            if (tvNoStatus.getVisibility() == View.VISIBLE && mediaList.isEmpty()) {
                initDiscovery();
            } else {
                showEmptyState(false, "", "", 0, false);
            }
        }

        if (mediaList != null && !mediaList.isEmpty()) {
            MediaStatusUtils.executor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                boolean changed = false;

                for (MediaItem item : mediaList) {
                    if (!item.isDownloaded()) {
                        String url = item.isVideo() ? item.getVideoUrl() : item.getUrl();
                        if (url == null) continue;

                        String fileName = "Pexels_" + Math.abs(url.hashCode()) + (item.isVideo() ? ".mp4" : ".jpg");

                        if (db.imageDao().isImageExists(fileName) || db.videoDao().isVideoExists(fileName)) {
                            item.setDownloaded(true);
                            changed = true;
                        }
                    }
                }

                if (changed && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (adapter != null) adapter.notifyDataSetChanged();
                    });
                }
            });
        }
    }
}