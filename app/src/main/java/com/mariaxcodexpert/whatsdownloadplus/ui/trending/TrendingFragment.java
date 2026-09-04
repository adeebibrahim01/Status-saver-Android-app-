package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.MediaStatusUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class TrendingFragment extends Fragment {

    private RecyclerView rvTrending;
    private ProgressBar pbLoading;
    private TrendingAdapter adapter;
    private ArrayList<TrendMediaItem> trendingList;
    private DatabaseReference mDatabase;

    private LinearLayout emptyState;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle, tvEmptyDesc;
    private MaterialButton btnRetry;

    private static final String TAG = "TrendingDebug";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_trending, container, false);

        initViews(root);
        setupFirebase();
        setupRecyclerView();

        btnRetry.setOnClickListener(v -> loadTrendingData());
        loadTrendingData();

        return root;
    }

    private void initViews(View root) {
        rvTrending = root.findViewById(R.id.rvTrending);
        pbLoading = root.findViewById(R.id.pbLoading);
        emptyState = root.findViewById(R.id.emptyState);
        ivEmptyIcon = root.findViewById(R.id.ivEmptyIcon);
        tvEmptyTitle = root.findViewById(R.id.tvEmptyTitle);
        tvEmptyDesc = root.findViewById(R.id.tvEmptyDesc);
        btnRetry = root.findViewById(R.id.btnRetry);
    }

    private void setupFirebase() {
        try {
            mDatabase = FirebaseDatabase.getInstance().getReference("trending_status");
            mDatabase.keepSynced(true);
        } catch (Exception e) {
            Log.e(TAG, "Firebase Init Error", e);
        }
    }

    private void setupRecyclerView() {
        trendingList = new ArrayList<>();
        rvTrending.setHasFixedSize(true);
        rvTrending.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new TrendingAdapter(trendingList, new TrendingAdapter.OnTrendItemClickListener() {
            @Override
            public void onSetStatus(TrendMediaItem item, TrendingAdapter.ViewHolder holder) {
                if (!isAdded() || getContext() == null) return;
                showAd(() -> {
                    if (item != null && item.getMediaUrl() != null) {
                        downloadToMediaStore(item.getMediaUrl(), item.getMediaType(), holder);
                    }
                });
            }

            @Override
            public void onPreview(TrendMediaItem item) {
                if (!isAdded() || getContext() == null) return;
                showAd(() -> {
                    if (item != null && !trendingList.isEmpty()) {
                        Intent intent = new Intent(getContext(), TrendingPreviewActivity.class);
                        intent.putExtra("MEDIA_LIST", trendingList);
                        intent.putExtra("POSITION", trendingList.indexOf(item));
                        startActivity(intent);
                    }
                });
            }
        });
        rvTrending.setAdapter(adapter);
    }

    private void loadTrendingData() {
        Context context = getContext();
        if (context == null) return;

        if (!isNetworkAvailable(context)) {

            showEmptyState(true,
                    getString(R.string.empty_title_no_connection),
                    getString(R.string.empty_desc_no_connection),
                    R.drawable.ic_wifi_off);
            return;
        }

        showEmptyState(false, "", "", 0);
        pbLoading.setVisibility(View.VISIBLE);

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                ArrayList<TrendMediaItem> tempList = new ArrayList<>();
                for (DataSnapshot countryNode : snapshot.getChildren()) {
                    String nodeKey = countryNode.getKey();
                    if (nodeKey == null || nodeKey.equalsIgnoreCase("metadata")) continue;

                    String fullCountryName = formatCountryName(nodeKey);

                    for (DataSnapshot data : countryNode.getChildren()) {
                        try {
                            TrendMediaItem item = data.getValue(TrendMediaItem.class);
                            if (item != null && !item.isVideo()) {
                                item.setId(data.getKey());
                                item.setCountry(fullCountryName);
                                tempList.add(item);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Parsing Error", e);
                        }
                    }
                }

                if (tempList.isEmpty()) {

                    showEmptyState(true,
                            getString(R.string.empty_title_no_records),
                            getString(R.string.empty_desc_no_records),
                            R.drawable.ic_photo_library_dashboard);
                } else {
                    syncDownloadStatusWithDB(tempList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    pbLoading.setVisibility(View.GONE);
                    showEmptyState(true,
                            getString(R.string.empty_title_server_error),
                            error.getMessage(),
                            R.drawable.ic_info_white);
                }
            }
        });
    }

    private String formatCountryName(String nodeKey) {
        if (nodeKey.equalsIgnoreCase("GLOBAL")) return getString(R.string.trend_region_global);
        try {
            Locale loc = new Locale("", nodeKey);
            String display = loc.getDisplayCountry();
            return (display.isEmpty() || display.equalsIgnoreCase(nodeKey)) ? nodeKey : display;
        } catch (Exception e) {
            return nodeKey;
        }
    }

    private void syncDownloadStatusWithDB(ArrayList<TrendMediaItem> items) {
        Context context = getContext();
        if (context == null) return;

        MediaStatusUtils.executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            for (TrendMediaItem item : items) {
                if (item.getMediaUrl() == null) continue;
                String fileName = "Trend_" + Math.abs(item.getMediaUrl().hashCode()) + (item.isVideo() ? ".mp4" : ".jpg");
                if (db.imageDao().isImageExists(fileName) || db.videoDao().isVideoExists(fileName)) {
                    item.setDownloaded(true);
                }
            }
            mainHandler.post(() -> {
                if (isAdded()) {
                    trendingList.clear();
                    trendingList.addAll(items);
                    adapter.notifyDataSetChanged();
                    pbLoading.setVisibility(View.GONE);
                }
            });
        });
    }

    private void downloadToMediaStore(String fileUrl, String type, TrendingAdapter.ViewHolder holder) {
        Context context = getContext();
        if (context == null || fileUrl == null) return;

        java.lang.ref.WeakReference<TrendingAdapter.ViewHolder> holderRef = new java.lang.ref.WeakReference<>(holder);

        if (holder.downloadOverlay != null) {
            holder.downloadOverlay.setVisibility(View.VISIBLE);
            holder.progressText.setText(getString(R.string.progress_percentage_format, 0));
            if (holder.pbCircular != null) holder.pbCircular.setProgress(0);
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(fileUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                int fileSize = connection.getContentLength();
                String extension = "video".equalsIgnoreCase(type) ? ".mp4" : ".jpg";
                String mimeType = "video".equalsIgnoreCase(type) ? "video/mp4" : "image/jpeg";
                boolean isVideo = "video".equalsIgnoreCase(type);

                String fileName = "Trend_" + Math.abs(fileUrl.hashCode()) + extension;
                java.io.File tempFile = new java.io.File(context.getCacheDir(), fileName);

                try (InputStream is = connection.getInputStream();
                     OutputStream fos = new java.io.FileOutputStream(tempFile)) {

                    byte[] buffer = new byte[8192];
                    int len;
                    long total = 0;

                    while ((len = is.read(buffer)) != -1) {
                        total += len;
                        int progress = fileSize > 0 ? (int) (total * 100 / fileSize) : 0;

                        TrendingAdapter.ViewHolder currentHolder = holderRef.get();
                        if (currentHolder != null) {
                            updateProgress(currentHolder, progress);
                        }
                        fos.write(buffer, 0, len);
                    }
                }

                saveToGalleryAndDB(tempFile, fileName, mimeType, isVideo, holderRef);

            } catch (Exception e) {
                Log.e("TrendingDownload", "Error: " + e.getMessage());
         mainHandler.post(() -> {
                    if (!isAdded()) return;
                    TrendingAdapter.ViewHolder currentHolder = holderRef.get();
                    if (currentHolder != null) {
                        currentHolder.downloadOverlay.setVisibility(View.GONE);
                        Toast.makeText(getContext(), getString(R.string.toast_download_failed_internet), Toast.LENGTH_SHORT).show();
                    }
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }


    private void updateProgress(TrendingAdapter.ViewHolder holder, int progress) {
        mainHandler.post(() -> {
            // FIX: Check if Fragment is still attached before UI update
            if (!isAdded() || getContext() == null) return;

            if (holder.progressText != null) holder.progressText.setText(getString(R.string.progress_percentage_format, progress));
            if (holder.pbCircular != null) holder.pbCircular.setProgress(progress);
        });
    }

    private void saveToGalleryAndDB(java.io.File tempFile, String fileName, String mimeType, boolean isVideo, java.lang.ref.WeakReference<TrendingAdapter.ViewHolder> holderRef) {
        Context context = getContext();
        if (context == null) return;

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, (isVideo ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES) + "/StatusSaver");
            }

            Uri collection = isVideo ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Uri uri = context.getContentResolver().insert(collection, values);

            if (uri != null) {
                try (InputStream in = new java.io.FileInputStream(tempFile);
                     OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                }

                MediaStatusUtils.saveToDatabase(context, uri, fileName, isVideo);

                mainHandler.post(() -> {
                    if (!isAdded() || getContext() == null) return;

                    TrendingAdapter.ViewHolder currentHolder = holderRef.get();
                    if (currentHolder != null) {
                        currentHolder.downloadOverlay.setVisibility(View.GONE);
                        currentHolder.btnSetStatus.setVisibility(View.GONE);
                        currentHolder.downloadStatus.setVisibility(View.VISIBLE);
                    }
                });
            }
        } catch (Exception e) {
            mainHandler.post(() -> {
                if (!isAdded()) return;
                TrendingAdapter.ViewHolder currentHolder = holderRef.get();
                if (currentHolder != null) currentHolder.downloadOverlay.setVisibility(View.GONE);
            });
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities cap = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return cap != null && (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        } else {
            return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
        }
    }

    private void showEmptyState(boolean show, String title, String desc, int iconRes) {
        if (!isAdded()) return;
        rvTrending.setVisibility(show ? View.GONE : View.VISIBLE);
        pbLoading.setVisibility(View.GONE);
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            tvEmptyTitle.setText(title);
            tvEmptyDesc.setText(desc);
            if (iconRes != 0) ivEmptyIcon.setImageResource(iconRes);
        }
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
}