package com.mariaxcodexpert.whatsdownloadplus.ui.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.Notifications10below.NotificationDatabaseHelper10below;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TrackerFragment extends Fragment {

    private static final String PREFS_NAME = "tracker_prefs";
    private static final String KEYWORDS_SET = "keywords_set";
    private static final String KEY_ACTIVE = "active_keyword";

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private FloatingActionButton fabAddKeyword;
    private trackingAdapter adapter;
    private NotificationDatabaseHelper10below dbHelper;

    private final List<trackingModel> displayedList = new ArrayList<>();
    private final List<String> keywordList = new ArrayList<>();
    private final Set<String> keywordSet = new HashSet<>();
    private String activeKeyword = "";
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private LottieAnimationView lottieEmptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_tracker, container, false);
        recyclerView = root.findViewById(R.id.recyclerViewTracker);
        emptyStateText = root.findViewById(R.id.textEmptyState);
        fabAddKeyword = root.findViewById(R.id.fabAddFilter);
        lottieEmptyState = root.findViewById(R.id.lottieEmptyState);
        swipeRefreshLayout = root.findViewById(R.id.swipeRefresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        dbHelper = new NotificationDatabaseHelper10below(requireContext());

        loadSavedKeywords();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadNotifications();
            swipeRefreshLayout.setRefreshing(false);
        });

        if (!keywordList.isEmpty()) {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            activeKeyword = prefs.getString(KEY_ACTIVE, keywordList.get(0));
        }

        adapter = new trackingAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        adapter.setHighlightKeyword(activeKeyword);

        loadNotifications();

        fabAddKeyword.setOnClickListener(v -> {
            fabAddKeyword.animate().rotationBy(360f).setDuration(400).start();
            showKeywordBottomSheet();
        });

        return root;
    }

    private void loadSavedKeywords() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> savedSet = prefs.getStringSet(KEYWORDS_SET, new HashSet<>());
        keywordSet.clear();
        if (savedSet != null) keywordSet.addAll(savedSet);
        keywordList.clear();
        keywordList.addAll(keywordSet);
    }

    private void saveKeywords() {
        SharedPreferences.Editor editor = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putStringSet(KEYWORDS_SET, new HashSet<>(keywordList));
        editor.putString(KEY_ACTIVE, activeKeyword);
        editor.apply();
    }

    private void showKeywordBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.sheet_add_keyword, null);

        EditText edtKeyword = sheetView.findViewById(R.id.inputKeyword);
        RecyclerView keywordsRecycler = sheetView.findViewById(R.id.sheetRecyclerKeywords);
        keywordsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        List<String> adapterList = new ArrayList<>(keywordList);
        final KeywordAdapter[] keywordAdapterHolder = new KeywordAdapter[1];

        keywordAdapterHolder[0] = new KeywordAdapter(adapterList, new KeywordAdapter.OnKeywordAction() {
            @Override
            public void onSelect(String keyword) {
                edtKeyword.setText(keyword);
                activeKeyword = keyword;
                saveKeywords();
                adapter.setHighlightKeyword(activeKeyword);
                loadNotifications();
            }

            @Override
            public void onEdit(int pos, String updated) {
                if (updated == null || updated.trim().isEmpty()) return;
                adapterList.set(pos, updated.trim());
                keywordList.clear();
                keywordList.addAll(adapterList);
                saveKeywords();
                adapter.setHighlightKeyword(activeKeyword);
                loadNotifications();
            }

            @Override
            public void onDelete(int pos) {
                if (pos < 0 || pos >= adapterList.size()) return;
                String removed = adapterList.remove(pos);
                keywordList.clear();
                keywordList.addAll(adapterList);

                if (removed.equals(activeKeyword)) {
                    activeKeyword = keywordList.isEmpty() ? "" : keywordList.get(0);
                    adapter.setHighlightKeyword(activeKeyword);
                    loadNotifications();
                }

                saveKeywords();
                keywordAdapterHolder[0].notifyItemRemoved(pos);
                Toast.makeText(getContext(), "Keyword deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReorder(List<String> newOrder) {
                keywordList.clear();
                keywordList.addAll(newOrder);
                saveKeywords();
            }
        });

        keywordsRecycler.setAdapter(keywordAdapterHolder[0]);

        sheetView.findViewById(R.id.btnSaveKeyword).setOnClickListener(v -> {
            String kw = edtKeyword.getText().toString().trim();
            if (!kw.isEmpty() && !keywordList.contains(kw)) {
                keywordList.add(0, kw);
                adapterList.clear();
                adapterList.addAll(keywordList);
                keywordAdapterHolder[0].notifyDataSetChanged();

                activeKeyword = kw;
                saveKeywords();
                adapter.setHighlightKeyword(activeKeyword);
                loadNotifications();

                edtKeyword.setText("");
            } else if (!kw.isEmpty()) {
                activeKeyword = kw;
                saveKeywords();
                adapter.setHighlightKeyword(activeKeyword);
                loadNotifications();
            }
        });

        sheetView.findViewById(R.id.btnExport).setOnClickListener(v -> exportMessagesToFile());

        sheet.setContentView(sheetView);
        sheet.show();
    }

    private void loadNotifications() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        displayedList.clear();

        // If no keyword is selected, show empty state
        if (activeKeyword == null || activeKeyword.isEmpty()) {
            adapter.updateList(displayedList);
            toggleEmptyState();
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        Cursor cursor = dbHelper.getAllNotifications();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        String sender = cursor.getString(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper10below.COLUMN_SENDER));
                        String message = cursor.getString(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper10below.COLUMN_MESSAGE));
                        long ts = cursor.getLong(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper10below.COLUMN_TIMESTAMP));

                        if (message != null && message.toLowerCase().contains(activeKeyword.toLowerCase())) {
                            String escapedKeyword = TextUtils.htmlEncode(activeKeyword);
                            String highlightedMessage = message.replaceAll("(?i)" + java.util.regex.Pattern.quote(activeKeyword),
                                    "<font color='#1B5E20'><b>" + escapedKeyword + "</b></font>");
                            displayedList.add(new trackingModel(sender, Html.fromHtml(highlightedMessage).toString(), ts));
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }

        Collections.sort(displayedList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        adapter.updateList(displayedList);
        toggleEmptyState();

        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
    }


    private void toggleEmptyState() {
        boolean empty = displayedList.isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyStateText.setVisibility(empty ? View.VISIBLE : View.GONE);
        lottieEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    public void onNewNotification(String sender, String message, long ts) {
        if (message == null || activeKeyword == null || activeKeyword.isEmpty()) return;

        if (message.toLowerCase().contains(activeKeyword.toLowerCase())) {

            // Check for duplicates
            boolean exists = false;
            for (trackingModel m : displayedList) {
                if (m.getSender().equals(sender) && m.getMessage().equals(message) && m.getTimestamp() == ts) {
                    exists = true;
                    break;
                }
            }
            if (exists) return; // skip if already added

            String escapedKeyword = TextUtils.htmlEncode(activeKeyword);
            String highlightedMessage = message.replaceAll("(?i)" + java.util.regex.Pattern.quote(activeKeyword),
                    "<font color='#1B5E20'><b>" + escapedKeyword + "</b></font>");

            displayedList.add(0, new trackingModel(sender, Html.fromHtml(highlightedMessage).toString(), ts));
            adapter.updateList(displayedList);
            toggleEmptyState();
        }
    }


    private void exportMessagesToFile() {
        try {
            File dir = requireContext().getExternalFilesDir(null);
            if (dir == null) dir = requireContext().getFilesDir();
            File file = new File(dir, "tracked_messages.txt");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            try (FileWriter fw = new FileWriter(file, false)) {
                for (trackingModel m : displayedList) {
                    fw.write("[" + sdf.format(new Date(m.getTimestamp())) + "] "
                            + m.getSender() + " — " + m.getMessage() + "\n\n");
                }
            }

            Toast.makeText(getContext(), "Exported to: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();

            androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(
                    androidx.core.content.FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".fileprovider",
                            file
                    ),
                    "text/plain"
            );
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(intent, "Open exported file");
            startActivity(chooser);

        } catch (Exception e) {
            Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


}