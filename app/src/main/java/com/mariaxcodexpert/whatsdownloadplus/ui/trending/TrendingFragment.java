package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mariaxcodexpert.whatsdownloadplus.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

public class TrendingFragment extends Fragment {

    private RecyclerView rvTrending;
    private ProgressBar pbLoading;
    private TrendingAdapter adapter;
    private ArrayList<TrendingModel> trendingList;
    private DatabaseReference mDatabase;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_trending, container, false);

        rvTrending = root.findViewById(R.id.rvTrending);
        pbLoading = root.findViewById(R.id.pbLoading);

        // Firebase Reference (Wahi path jo Python script mein hai)
        mDatabase = FirebaseDatabase.getInstance().getReference("trending_status");

        setupRecyclerView();
        loadTrendingData();

        return root;
    }

    private void setupRecyclerView() {
        trendingList = new ArrayList<>();
        rvTrending.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TrendingAdapter(trendingList, item -> {
            // Direct share triggered from adapter
            downloadAndShare(item.getVideoUrl(), item.getMediaType());
        });

        rvTrending.setAdapter(adapter);
    }

    private void loadTrendingData() {
        pbLoading.setVisibility(View.VISIBLE);

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                trendingList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    TrendingModel model = data.getValue(TrendingModel.class);
                    if (model != null) {
                        trendingList.add(model);
                    }
                }
                // Trends ko reverse kar dete hain taake latest upar aaye
                Collections.reverse(trendingList);
                adapter.notifyDataSetChanged();
                pbLoading.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadAndShare(String fileUrl, String type) {
        Toast.makeText(getContext(), "AI is preparing your status...", Toast.LENGTH_SHORT).show();

        // Background Thread mein download logic
        new Thread(() -> {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // Cache folder mein temporary file banana
                String fileName = "temp_status_" + System.currentTimeMillis() + (type.equals("video") ? ".mp4" : ".jpg");
                File cachePath = new File(getContext().getCacheDir(), "trending");
                cachePath.mkdirs();
                File tempFile = new File(cachePath, fileName);

                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(tempFile);

                byte[] buffer = new byte[1024];
                int len;
                while ((len = input.read(buffer)) > 0) {
                    output.write(buffer, 0, len);
                }
                output.close();
                input.close();

                // UI Thread par share intent trigger karna
                new Handler(Looper.getMainLooper()).post(() -> {
                    triggerWhatsApp(tempFile, type);
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void triggerWhatsApp(File file, String type) {
        Uri contentUri = FileProvider.getUriForFile(getContext(),
                getContext().getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(type.equals("video") ? "video/mp4" : "image/jpeg");
        intent.setPackage("com.whatsapp"); // Direct WhatsApp target
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "WhatsApp not found", Toast.LENGTH_SHORT).show();
        }
    }
}