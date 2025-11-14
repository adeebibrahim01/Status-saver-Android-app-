package com.mariaxcodexpert.whatsdownloadplus.ui.prediction;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.ContactEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.StatusDao;
import com.mariaxcodexpert.whatsdownloadplus.data.StatusEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatusPredictionFragment extends Fragment {

    private RecyclerView recyclerView;
    private StatusPredictionAdapter adapter;
    private AppDatabase db;
    private StatusDao statusDao;
    private Handler handler = new Handler();
    private Runnable refreshRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_status_prediction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.predictionRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize Room
        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "statusDB")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries() // Only for demo; background preferred
                .build();
        statusDao = db.statusDao();

        // Load predictions immediately
        loadPredictions();

        // Auto-refresh every 5 seconds
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadPredictions();
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(refreshRunnable);
    }

    private void loadPredictions() {
        List<ContactEntity> contacts = statusDao.getAllContacts();
        if (contacts.isEmpty()) {
            Toast.makeText(getContext(), "No contacts found", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> predictedTypes = new ArrayList<>();
        List<String> predictedTimes = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (ContactEntity contact : contacts) {
            List<StatusEntity> statuses = statusDao.getStatusHistory(contact.id);
            if (statuses.isEmpty()) {
                predictedTypes.add("No status yet");
                predictedTimes.add("-");
                continue;
            }

            // --- Most frequent status type ---
            List<String> types = new ArrayList<>();
            for (StatusEntity s : statuses) types.add(s.type);
            String mostFrequentType = types.get(0);
            int maxCount = 0;
            for (String t : types) {
                int count = Collections.frequency(types, t);
                if (count > maxCount) {
                    maxCount = count;
                    mostFrequentType = t;
                }
            }

            // --- Calculate average interval between statuses ---
            long avgInterval = 3600 * 1000; // default 1 hour
            if (statuses.size() > 1) {
                long totalDiff = 0;
                for (int i = 0; i < statuses.size() - 1; i++) {
                    totalDiff += Math.abs(statuses.get(i).timestamp - statuses.get(i + 1).timestamp);
                }
                avgInterval = totalDiff / (statuses.size() - 1);
            }

            // --- Predict next status time ---
            long lastTimestamp = statuses.get(0).timestamp;
            long nextPrediction = lastTimestamp + avgInterval;

            predictedTypes.add(mostFrequentType);
            predictedTimes.add(sdf.format(new Date(nextPrediction)));
        }

        // Update RecyclerView adapter
        if (adapter == null) {
            adapter = new StatusPredictionAdapter(getContext(), contacts, predictedTypes, predictedTimes);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateData(contacts, predictedTypes, predictedTimes);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(refreshRunnable);
    }
}
