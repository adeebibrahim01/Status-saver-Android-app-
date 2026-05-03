package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import java.util.Set;
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

    private final String API_KEY = "XBwe0uVt6gSChcA14IxZyGGkT3wiQCY79vYW3QbSVJOZbg0aLajuaDBK";
    private int currentPage = 1;
    private boolean isLoading = false;
    private String currentQuery = "";

    private SharedPreferences prefs;
    private static final String PREF_NAME = "UserInterests";

    private final String[] trendingTags = {"4k Wallpaper", "Luxury Cars", "Abstract Art", "Nature HD", "Aesthetic Dark", "Cyberpunk", "Ocean 8k", "Minimalist"};
    private TextView Headertext ;
    private FirebaseAnalytics mFirebaseAnalytics;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_online_search, container, false);

        etSearch = root.findViewById(R.id.etSearch);
        recyclerView = root.findViewById(R.id.rvOnlineImages);
        loader = root.findViewById(R.id.loader);
        tvNoStatus = root.findViewById(R.id.tvNoStatus);
        Headertext =root.findViewById(R.id.innerSubtitle);
       // proTypewriter(Headertext, "Status Discovery");
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
                if (mediaList.size() == items.size()) {
                    for (int i = 0; i < items.size(); i++) {
                        if (mediaList.get(i).isDownloaded() != items.get(i).isDownloaded()) {
                            mediaList.get(i).setDownloaded(items.get(i).isDownloaded());
                            if (adapter != null) adapter.notifyItemChanged(i);
                        }
                    }
                } else {
                    mediaList.clear();
                    mediaList.addAll(items);
                    if (adapter != null) adapter.notifyDataSetChanged();
                }
            }
        });

        initDiscovery();


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

        // 🔥 UPDATE: Default animator wapas krain taake layout animation smoothly kaam kray
        recyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());

        recyclerView.setHasFixedSize(true);

        // 🔥 UPDATE: Waterfall animation controller ko load kero
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

        // 🔥 Direct UI Update: Download start hote hi overlay dikhao
        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
        if (holder != null) {
            holder.itemView.findViewById(R.id.downloadOverlay).setVisibility(View.VISIBLE);
            ((TextView) holder.itemView.findViewById(R.id.progressText)).setText("0%");
        }

        MediaStatusUtils.executor.execute(() -> {
            try {
                String downloadUrl = item.isVideo() ? item.getVideoUrl() : item.getUrl();
                String fileName = "Pexels_" + Math.abs(downloadUrl.hashCode()) + (item.isVideo() ? ".mp4" : ".jpg");

                // Fake progress feel dene k liye updates
                for (int p = 15; p <= 85; p += 25) {
                    final int progress = p;
                    if (getActivity() != null) {
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
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                // 🔥 UI Clean-up: Overlay hatao aur status update kero
                                if (holder != null) {
                                    holder.itemView.findViewById(R.id.downloadOverlay).setVisibility(View.GONE);
                                }

                                if (success) {
                                    item.setDownloaded(true);
                                    viewModel.updateDownloadStatus(downloadUrl, true);
                                    if (adapter != null && position != -1) {
                                        adapter.notifyItemChanged(position);
                                    }
                                    Toast.makeText(getContext(), "Saved Successfully! ✧", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("DownloadError", "Error downloading: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (holder != null) {
                            holder.itemView.findViewById(R.id.downloadOverlay).setVisibility(View.GONE);
                        }
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
        // 1. Internet Check pehle krain
        if (!isNetworkAvailable()) {
            showNoInternetUI();
            return;
        }

        if (isNewSearch && isAdded()) {
            loader.setVisibility(View.VISIBLE);
            tvNoStatus.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE); // Naya search h to purana data hide krain
        }

        if (query == null || query.trim().isEmpty()) {
            query = "trending";
        }

        String encodedQuery = Uri.encode(query.trim());
        OkHttpClient client = new OkHttpClient();

        // --- Photos API Call ---
        String photoUrl = "https://api.pexels.com/v1/search?query=" + encodedQuery + "&per_page=30&page=" + currentPage + "&orientation=portrait";
        try {
            Request photoReq = new Request.Builder()
                    .url(photoUrl)
                    .addHeader("Authorization", API_KEY)
                    .build();

            client.newCall(photoReq).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    handleApiError(isNewSearch);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    List<MediaItem> photosList = new ArrayList<>();
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject json = new JSONObject(response.body().string());
                            JSONArray photos = json.getJSONArray("photos");
                            for (int i = 0; i < photos.length(); i++) {
                                JSONObject pObj = photos.getJSONObject(i);
                                photosList.add(new MediaItem(pObj.getJSONObject("src").getString("large2x"), null, false));
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    } else {
                        handleApiError(isNewSearch); // Server error handle krain
                    }
                    updateUI(photosList, isNewSearch);
                }
            });
        } catch (Exception e) {
            handleApiError(isNewSearch);
        }

        // --- Videos API Call ---
        String videoUrl = "https://api.pexels.com/videos/search?query=" + encodedQuery + "&per_page=15&page=" + currentPage + "&orientation=portrait";
        try {
            Request videoReq = new Request.Builder()
                    .url(videoUrl)
                    .addHeader("Authorization", API_KEY)
                    .build();

            client.newCall(videoReq).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Video fail b ho jaye to Photo show ho sakti h, isliye silent error handling
                    checkAndHideLoader(isNewSearch);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    List<MediaItem> videosList = new ArrayList<>();
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject json = new JSONObject(response.body().string());
                            JSONArray videos = json.getJSONArray("videos");
                            for (int i = 0; i < videos.length(); i++) {
                                JSONObject vObj = videos.getJSONObject(i);
                                String vUrl = vObj.getJSONArray("video_files").getJSONObject(0).getString("link");
                                videosList.add(new MediaItem(vObj.getString("image"), vUrl, true));
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    updateUI(videosList, false);
                }
            });
        } catch (Exception e) {
            Log.e("OkHttpError", "Invalid Video URL");
        }
    }

    // 🔥 Helper Method: API Failure handle karne k liye
    private void handleApiError(boolean isNewSearch) {
        if (isNewSearch && isAdded()) {
            getActivity().runOnUiThread(() -> {
                loader.setVisibility(View.GONE);
                if (mediaList.isEmpty()) {
                    tvNoStatus.setVisibility(View.VISIBLE);

                    TextView tvTitle = tvNoStatus.findViewById(R.id.tvEmptyTitle);

                    if (tvTitle != null) tvTitle.setText("Something went wrong ⚠️");
                }
            });
        }
    }
    private void showNoInternetUI() {
        if (!isAdded()) return;
        getActivity().runOnUiThread(() -> {
            loader.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            tvNoStatus.setVisibility(View.VISIBLE);

            TextView tvTitle = tvNoStatus.findViewById(R.id.tvEmptyTitle);
            TextView tvDesc = tvNoStatus.findViewById(R.id.tvEmptyDescription);

            if (tvTitle != null) tvTitle.setText("No Connection");
            if (tvDesc != null) tvDesc.setText("Check your internet and try again.");

        });
    }
    private void checkAndHideLoader(boolean isNewSearch) {
        if (isNewSearch && isAdded()) {
            getActivity().runOnUiThread(() -> loader.setVisibility(View.GONE));
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
    private void fetchVideos(OkHttpClient client, String query, List<MediaItem> tempMedia, boolean isNewSearch) {
        String videoUrl = "[https://api.pexels.com/videos/search?query=](https://api.pexels.com/videos/search?query=)" + query + "&per_page=15&page=" + currentPage + "&orientation=portrait";
        Request videoReq = new Request.Builder().url(videoUrl).addHeader("Authorization", API_KEY).build();

        client.newCall(videoReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { updateUI(tempMedia, isNewSearch); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray videos = json.getJSONArray("videos");
                        for (int i = 0; i < videos.length(); i++) {
                            JSONObject vObj = videos.getJSONObject(i);
                            String vUrl = vObj.getJSONArray("video_files").getJSONObject(0).getString("link");
                            tempMedia.add(new MediaItem(vObj.getString("image"), vUrl, true));
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
                updateUI(tempMedia, isNewSearch);
            }
        });
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (!query.isEmpty()) {

            // --- FIREBASE SEARCH TRACKING ---
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, query);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle);
            // --------------------------------

            psychologyManager.trackSearch(query);
            currentQuery = query;
            currentPage = 1;
            isLoading = true;

            // 🔥 UPDATE: INVISIBLE use krain taake layout apni jagah rahay par nazar na aaye
            // Alpha 0f is liye taake naye waterfall k waqt items "fade-in" ho sakein
            if (recyclerView != null) {
                recyclerView.setVisibility(View.INVISIBLE);
                recyclerView.setAlpha(0f);
            }

            // Data saaf krain taake naya result fresh lagay
            mediaList.clear();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            updateToolbarUI(query);

            // Loader show kero (Center mein premium look k liye)
            if (loader != null) {
                loader.setVisibility(View.VISIBLE);
            }

            if (tvNoStatus != null) {
                tvNoStatus.setVisibility(View.GONE);
            }

            // API call trigger krain
            fetchMixedContent(query, true);

            // Keyboard hide kerne k liye focus hatayein aur keyboard close kero
            etSearch.clearFocus();
            hideKeyboard();
        }
    }

    // Keyboard hide kerne k liye helper (Optional but recommended for Premium UX)
    private void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private void updateUI(List<MediaItem> newItems, boolean isNewSearch) {
        if (!isAdded() || newItems == null) return;

        MediaStatusUtils.executor.execute(() -> {
            // Database check logic (Already downloaded status update)
            AppDatabase db = AppDatabase.getInstance(getContext());
            for (MediaItem item : newItems) {
                String url = item.isVideo() ? item.getVideoUrl() : item.getUrl();
                String fileName = "Pexels_" + Math.abs(url.hashCode()) + (item.isVideo() ? ".mp4" : ".jpg");
                if (db.imageDao().isImageExists(fileName) || db.videoDao().isVideoExists(fileName)) {
                    item.setDownloaded(true);
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (loader != null) loader.setVisibility(View.GONE);

                    int startPos = mediaList.size();

                    if (isNewSearch) {
                        mediaList.clear();
                        mediaList.addAll(newItems);
                        Collections.shuffle(mediaList);

                        // 1. Data pehle notify krain
                        adapter.notifyDataSetChanged();

                        // 2. 🔥 FIX: Alpha ko 1 kerna aur visibility set kerna
                        if (!mediaList.isEmpty()) {
                            recyclerView.setVisibility(View.VISIBLE);
                            recyclerView.setAlpha(1.0f); // Blank screen fix yahan h

                            // 3. 🔥 Waterfall effect trigger krain
                            recyclerView.scheduleLayoutAnimation();
                        }
                    } else {
                        // Pagination (Scrolling) k waqt animation ki zaroorat nahi hoti
                        mediaList.addAll(newItems);
                        adapter.notifyItemRangeInserted(startPos, newItems.size());

                        // Extra safety for visibility
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.setAlpha(1.0f);
                    }

                    // ViewModel update krain taake state save rahay
                    viewModel.setMediaList(new ArrayList<>(mediaList));

                    // Empty message handling
                    if (tvNoStatus != null) {
                        tvNoStatus.setVisibility(mediaList.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    isLoading = false;
                });
            }
        });
    }
    private void initDiscovery() {
        currentQuery = psychologyManager.getMixedRecommendedQuery(trendingTags);
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
        if (mediaList != null && !mediaList.isEmpty()) {
            MediaStatusUtils.executor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(getContext());
                for (int i = 0; i < mediaList.size(); i++) {
                    MediaItem item = mediaList.get(i);
                    if (!item.isDownloaded()) {
                        String url = item.isVideo() ? item.getVideoUrl() : item.getUrl();
                        String fileName = "Pexels_" + Math.abs(url.hashCode()) + (item.isVideo() ? ".mp4" : ".jpg");
                        if (db.imageDao().isImageExists(fileName) || db.videoDao().isVideoExists(fileName)) {
                            item.setDownloaded(true);
                            final int position = i;
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (adapter != null) adapter.notifyItemChanged(position);
                                });
                            }
                        }
                    }
                }
            });
        }
    }
}