package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mariaxcodexpert.whatsdownloadplus.R;
import java.util.ArrayList;

public class TrendingFragment extends Fragment {

    private RecyclerView rvTrending;
    private ProgressBar pbLoading;
    private TrendingAdapter adapter;
    private ArrayList<TrendingModel> trendingList;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_trending, container, false);

        rvTrending = root.findViewById(R.id.rvTrending);
        pbLoading = root.findViewById(R.id.pbLoading);

        setupRecyclerView();
        loadTrendingData();

        return root;
    }

    private void setupRecyclerView() {
        trendingList = new ArrayList<>();
        rvTrending.setLayoutManager(new LinearLayoutManager(getContext()));

        // Adapter listener to handle direct share
        adapter = new TrendingAdapter(trendingList, item -> {
            shareDirectToWhatsApp(item.getVideoUrl());
        });

        rvTrending.setAdapter(adapter);
    }

    private void loadTrendingData() {
        pbLoading.setVisibility(View.VISIBLE);

        // TODO: Yahan hum Firebase se Google Trend wala data fetch karenge
        // Filhal ke liye simple logic
        pbLoading.setVisibility(View.GONE);
    }

    private void shareDirectToWhatsApp(String url) {
        // Bhai, yahan hum temporary download logic aur Intent trigger karenge
        Toast.makeText(getContext(), "Preparing Status...", Toast.LENGTH_SHORT).show();

        // Direct WhatsApp Intent Logic (Jaisa humne discuss kiya tha)
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/mp4");
        intent.setPackage("com.whatsapp");
        // intent.putExtra(Intent.EXTRA_STREAM, tempUri);
        startActivity(Intent.createChooser(intent, "Share via"));
    }
}