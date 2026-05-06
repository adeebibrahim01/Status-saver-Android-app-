package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import android.content.ContentValues;
import android.content.Intent;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.MediaStatusUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class TrendingFragment extends Fragment {

    private RecyclerView rvTrending;
    private ProgressBar pbLoading;
    private TrendingAdapter adapter;
    private ArrayList<TrendMediaItem> trendingList;
    private DatabaseReference mDatabase;
    private static final String TAG = "TrendingDebug";

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_trending, container, false);

        rvTrending = root.findViewById(R.id.rvTrending);
        pbLoading = root.findViewById(R.id.pbLoading);

        mDatabase = FirebaseDatabase.getInstance().getReference("trending_status");
        mDatabase.keepSynced(true);

        setupRecyclerView();
        loadTrendingData();
        return root;
    }

    private void setupRecyclerView() {
        trendingList = new ArrayList<>();
        rvTrending.setHasFixedSize(true);
        rvTrending.setItemViewCacheSize(30);
        rvTrending.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new TrendingAdapter(trendingList, new TrendingAdapter.OnTrendItemClickListener() {
            @Override
            public void onSetStatus(TrendMediaItem item, TrendingAdapter.ViewHolder holder) {
                // 🔥 Requirement 1: Show Ad before Download
                showAd(() -> {
                    if (item != null && item.getMediaUrl() != null) {
                        downloadToMediaStore(item.getMediaUrl(), item.getMediaType(), holder);
                    }
                });
            }

            @Override
            public void onPreview(TrendMediaItem item) {
                // 🔥 Requirement 1: Show Ad before Preview
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

    // 🔥 Ad Helper Method
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

    private void loadTrendingData() {
        pbLoading.setVisibility(View.VISIBLE);
        mDatabase.child("GLOBAL").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                ArrayList<TrendMediaItem> tempList = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        try {
                            TrendMediaItem item = data.getValue(TrendMediaItem.class);
                            if (item != null) {
                                item.setId(data.getKey());
                                tempList.add(item);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Parsing Error", e);
                        }
                    }
                }
                syncDownloadStatusWithDB(tempList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) pbLoading.setVisibility(View.GONE);
            }
        });
    }

    // 🔥 Database Sync: Check if already downloaded
    private void syncDownloadStatusWithDB(ArrayList<TrendMediaItem> items) {
        MediaStatusUtils.executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            for (TrendMediaItem item : items) {
                String fileName = "Trend_" + Math.abs(item.getMediaUrl().hashCode()) + (item.isVideo() ? ".mp4" : ".jpg");
                if (db.imageDao().isImageExists(fileName) || db.videoDao().isVideoExists(fileName)) {
                    item.setDownloaded(true);
                }
            }
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                    trendingList.clear();
                    trendingList.addAll(items);
                    adapter.notifyDataSetChanged();
                    pbLoading.setVisibility(View.GONE);
                });
            }
        });
    }

    private void downloadToMediaStore(String fileUrl, String type, TrendingAdapter.ViewHolder holder) {
        if (getContext() == null) return;

        // 🔥 UI Update: Loader dikhao aur Progress Bar ko real-time mode par set kero
        new Handler(Looper.getMainLooper()).post(() -> {
            if (holder.downloadOverlay != null) {
                holder.downloadOverlay.setVisibility(View.VISIBLE);
                holder.progressText.setText("0%");

                if (holder.pbCircular != null) {
                    // Progress mode ko manual (non-spinning) kerna zaroori hai fill dikhane ke liye
                    holder.pbCircular.setIndeterminate(false);
                    holder.pbCircular.setMax(100);
                    holder.pbCircular.setProgress(0);
                }
            }
        });

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                boolean isVideo = "video".equalsIgnoreCase(type);
                String extension = isVideo ? ".mp4" : ".jpg";
                String fileName = "Trend_" + Math.abs(fileUrl.hashCode()) + extension;
                String mimeType = isVideo ? "video/mp4" : "image/jpeg";

                java.io.File tempFile = new java.io.File(requireContext().getCacheDir(), fileName);

                URL url = new URL(fileUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.connect();

                int fileSize = connection.getContentLength();

                try (InputStream is = connection.getInputStream();
                     OutputStream fos = new java.io.FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    long total = 0;
                    while ((len = is.read(buffer)) != -1) {
                        total += len;
                        if (fileSize > 0) {
                            int progress = (int) (total * 100 / fileSize);

                            // 🔥 UI Thread par Text aur Circle dono ko update kerna
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (holder.progressText != null) {
                                    holder.progressText.setText(progress + "%");
                                }
                                if (holder.pbCircular != null) {
                                    // Smooth fill ke liye logic
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                        holder.pbCircular.setProgress(progress, true);
                                    } else {
                                        holder.pbCircular.setProgress(progress);
                                    }
                                }
                            });
                        }
                        fos.write(buffer, 0, len);
                    }
                }

                // Download mukammal: Ab database aur gallery mein save kero
                saveToGalleryAndDB(tempFile, fileName, mimeType, isVideo, holder);

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (holder.downloadOverlay != null) holder.downloadOverlay.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Download Failed", Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void saveToGalleryAndDB(java.io.File tempFile, String fileName, String mimeType, boolean isVideo, TrendingAdapter.ViewHolder holder) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

            // Requirement 3: Save to "StatusSaver" folder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String subDir = isVideo ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES;
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, subDir + "/StatusSaver");
            }

            Uri collection = isVideo ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Uri uri = requireContext().getContentResolver().insert(collection, values);

            if (uri != null) {
                try (InputStream in = new java.io.FileInputStream(tempFile);
                     OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                }

                // Requirement 2: Save record to local Database
                MediaStatusUtils.saveToDatabase(requireContext(), uri, fileName, isVideo);

                new Handler(Looper.getMainLooper()).post(() -> {
                    // 1. 🔥 Pehle progress/loader ko khatam kero
                    if (holder.downloadOverlay != null) {
                        holder.downloadOverlay.setVisibility(View.GONE);
                    }

                    // 2. 🔥 Ab purana button hide kero (Jo download ke doran visible tha)
                    if (holder.btnSetStatus != null) {
                        holder.btnSetStatus.setVisibility(View.GONE);
                    }

                    // 3. 🔥 Ab downloaded (Done) icon dekhao
                    if (holder.downloadStatus != null) {
                        holder.downloadStatus.setVisibility(View.VISIBLE);
                    }

                    Toast.makeText(getContext(), "Saved to StatusSaver! ✧", Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            Log.e("SaveError", e.getMessage());
            // Error ki surat mein loader hata do taake UI stuck na rahe
            new Handler(Looper.getMainLooper()).post(() -> {
                if (holder.downloadOverlay != null) holder.downloadOverlay.setVisibility(View.GONE);
            });
        }
    }
}