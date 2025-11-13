package com.mariaxcodexpert.whatsdownloadplus.ui.tracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.Notifications.NotificationAdapter;
import com.mariaxcodexpert.whatsdownloadplus.ui.Notifications.NotificationDatabaseHelper;
import com.mariaxcodexpert.whatsdownloadplus.ui.Notifications.NotificationModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrackerFragment extends Fragment {

    private static final String PREFS_NAME = "tracker_prefs";
    private static final String KEYWORDS_SET = "keywords_set";

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddKeyword;
    private NotificationAdapter adapter;
    private NotificationDatabaseHelper dbHelper;
    private List<NotificationModel> displayedList = new ArrayList<>();
    private Set<String> keywordSet = new HashSet<>();
    private String activeKeyword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_tracker, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewTracker);
        fabAddKeyword = root.findViewById(R.id.fabAddFilter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dbHelper = new NotificationDatabaseHelper(requireContext());

        // Load saved keywords
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        keywordSet = prefs.getStringSet(KEYWORDS_SET, new HashSet<>());

        // Initial load: if any keywords exist, show first one by default
        if (!keywordSet.isEmpty()) {
            activeKeyword = keywordSet.iterator().next();
            Toast.makeText(getContext(), "Tracking: " + activeKeyword, Toast.LENGTH_SHORT).show();
        }

        loadNotifications();

        fabAddKeyword.setOnClickListener(v -> showKeywordDialog());

        return root;
    }

    private void showKeywordDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_tracker_keywords, null);

        EditText editTextNewKeyword = dialogView.findViewById(R.id.editTextNewKeyword);
        RecyclerView recyclerViewKeywords = dialogView.findViewById(R.id.recyclerViewKeywords);
        recyclerViewKeywords.setLayoutManager(new LinearLayoutManager(getContext()));

        List<String> keywordList = new ArrayList<>(keywordSet);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Manage Keywords")
                .setView(dialogView)
                .setPositiveButton("Add", null) // We override later
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        KeywordAdapter keywordAdapter = new KeywordAdapter(keywordList, keyword -> {
            // On keyword click: set active, save, close dialog
            activeKeyword = keyword;

            // Save selected keyword
            SharedPreferences.Editor editor = requireContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit();
            editor.putStringSet(KEYWORDS_SET, new HashSet<>(keywordSet));
             // ensure all keywords saved
            editor.apply();

            loadNotifications();
            Toast.makeText(getContext(), "Tracking: " + activeKeyword, Toast.LENGTH_SHORT).show();

            dialog.dismiss(); // close popup immediately
        });

        recyclerViewKeywords.setAdapter(keywordAdapter);

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newKeyword = editTextNewKeyword.getText().toString().trim();
                if (!newKeyword.isEmpty() && !keywordSet.contains(newKeyword)) {
                    keywordSet.add(newKeyword);

                    // Save keywords
                    SharedPreferences.Editor editor = requireContext()
                            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit();
                    editor.putStringSet(KEYWORDS_SET, new java.util.HashSet<>(keywordSet));
                    editor.apply();

                    // Update adapter
                    keywordList.clear();
                    keywordList.addAll(keywordSet);
                    keywordAdapter.notifyDataSetChanged();

                    activeKeyword = newKeyword;
                    loadNotifications();
                    Toast.makeText(getContext(), "Tracking: " + newKeyword, Toast.LENGTH_SHORT).show();

                    dialog.dismiss(); // close popup after adding new keyword
                } else {
                    Toast.makeText(getContext(), "Keyword already exists or empty", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }


    public void loadNotifications() {
        displayedList.clear();
        if (activeKeyword.isEmpty()) return;

        Cursor cursor = dbHelper.getAllNotifications();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String sender = cursor.getString(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper.COLUMN_SENDER));
                String message = cursor.getString(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper.COLUMN_MESSAGE));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper.COLUMN_TIMESTAMP));

                // Only add messages containing the active keyword
                if (message.toLowerCase().contains(activeKeyword.toLowerCase())) {
                    displayedList.add(new NotificationModel(sender, message, timestamp));
                }

            } while (cursor.moveToNext());
            cursor.close();
        }

        if (adapter == null) {
            adapter = new NotificationAdapter(displayedList);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(displayedList);
        }
    }

    /**
     * Call this from NotificationListener whenever a new notification arrives
     */
    public void onNewNotification(String sender, String message, long timestamp) {
        for (String keyword : keywordSet) {
            if (message.toLowerCase().contains(keyword.toLowerCase())) {
                // If the keyword matches the currently active one, display it
                if (keyword.equals(activeKeyword)) {
                    displayedList.add(0, new NotificationModel(sender, message, timestamp));
                    if (adapter != null) adapter.updateList(displayedList);
                }
                break;
            }
        }
    }
}
