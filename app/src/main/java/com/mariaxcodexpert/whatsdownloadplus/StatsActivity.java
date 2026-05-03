package com.mariaxcodexpert.whatsdownloadplus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsActivity extends AppCompatActivity {

    private TextView tvTerminal;
    private ScrollView scrollView;
    private final StringBuilder logBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        tvTerminal = findViewById(R.id.tvTerminal);
        scrollView = findViewById(R.id.scrollViewTerminal);

        tvTerminal.setText("Fetching Database Records...\n");

        fetchDatabaseRecords();
    }

    private void fetchDatabaseRecords() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            // Dono tables ka complete record uthana
            List<ImageEntity> allImages = db.imageDao().getAllImagesSync();
            List<VideoEntity> allVideos = db.videoDao().getAllVideosSync();

            logBuilder.append("--- IMAGES_TABLE RECORDS ---\n");
            if (allImages.isEmpty()) {
                logBuilder.append("No records found in Image table.\n");
            } else {
                for (ImageEntity img : allImages) {
                    logBuilder.append("FILE: ").append(img.fileName).append("\n");
                    logBuilder.append("SAVED: ").append(img.isDownloaded ? "YES" : "NO").append("\n");
                    logBuilder.append("PATH: ").append(img.gallery_path != null ? img.gallery_path : "N/A").append("\n");
                    logBuilder.append("---------------------------\n");
                }
            }

            logBuilder.append("\n\n--- VIDEOS_TABLE RECORDS ---\n");
            if (allVideos.isEmpty()) {
                logBuilder.append("No records found in Video table.\n");
            } else {
                for (VideoEntity vid : allVideos) {
                    logBuilder.append("FILE: ").append(vid.fileName).append("\n");
                    logBuilder.append("SAVED: ").append(vid.isDownloaded ? "YES" : "NO").append("\n");
                    logBuilder.append("PATH: ").append(vid.gallery_path != null ? vid.gallery_path : "N/A").append("\n");
                    logBuilder.append("---------------------------\n");
                }
            }

            // UI Update kerna
            new Handler(Looper.getMainLooper()).post(() -> {
                tvTerminal.setText(logBuilder.toString());
                // Scroll to bottom
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            });
        });
    }
}