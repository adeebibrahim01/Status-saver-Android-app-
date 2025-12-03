package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications10below;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsFragment10below extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private EditText searchEditText;
    private LottieAnimationView lottieEmptyState;

    private NotificationAdapter10below adapter;
    private NotificationDatabaseHelper10below dbHelper;

    private final List<NotificationModel10below> notificationList = new ArrayList<>();
    private final List<NotificationModel10below> filteredList = new ArrayList<>();

    private BroadcastReceiver notificationReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerView = view.findViewById(R.id.recyclerViewNotifications);
        emptyText = view.findViewById(R.id.emptyText);
        lottieEmptyState = view.findViewById(R.id.lottieEmptyState);
        searchEditText = view.findViewById(R.id.searchEditText);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dbHelper = new NotificationDatabaseHelper10below(requireContext());

        loadNotifications();
        setupSwipeToDelete();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadNotifications();
            swipeRefreshLayout.setRefreshing(false);
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotifications(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // ------------------ Broadcast receiver for new notifications ------------------
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String sender = intent.getStringExtra("sender");
                String chatId = intent.getStringExtra("chatId");
                String message = intent.getStringExtra("message");
                long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());

                if (!TextUtils.isEmpty(sender) && !TextUtils.isEmpty(message)) {
                    insertAndShow(sender, chatId, message, timestamp);
                }
            }
        };
        requireContext().registerReceiver(notificationReceiver, new IntentFilter("com.mariaxcodexpert.NEW_NOTIFICATION"));

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationReceiver != null) {
            requireContext().unregisterReceiver(notificationReceiver);
            notificationReceiver = null;
        }
    }

    // ------------------- Load notifications from DB -------------------
    private void loadNotifications() {
        Cursor cursor = dbHelper.getAllNotifications();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper10below.COLUMN_ID));
                String sender = cursor.getString(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper10below.COLUMN_SENDER));
                String chatId = cursor.getString(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper10below.COLUMN_CHAT_ID));
                String message = cursor.getString(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper10below.COLUMN_MESSAGE));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper10below.COLUMN_TIMESTAMP));

                // Only add if not already in list
                boolean alreadyExists = false;
                for (NotificationModel10below n : notificationList) {
                    if (n.getChatId().equals(chatId) && n.getMessage().equals(message)) {
                        alreadyExists = true;
                        break;
                    }
                }
                if (!alreadyExists) {
                    notificationList.add(new NotificationModel10below(id, sender, message, timestamp, chatId));
                }

            } while (cursor.moveToNext());
            cursor.close();
        }

        // Sort newest first
        Collections.sort(notificationList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        filteredList.clear();
        filteredList.addAll(notificationList);
        updateRecyclerView();
    }

    // ------------------- Insert new notification -------------------
    private void insertAndShow(String sender, String chatId, String message, long timestamp) {
        // Prevent duplicate
        boolean alreadyExists = false;
        for (NotificationModel10below n : notificationList) {
            if (n.getChatId().equals(chatId) && n.getMessage().equals(message)) {
                alreadyExists = true;
                break;
            }
        }
        if (alreadyExists) return;

        long id = dbHelper.insertNotificationWithChatId(sender, chatId, message, timestamp);
        if (id != -1) {
            NotificationModel10below newMsg = new NotificationModel10below(id, sender, message, timestamp, chatId);
            notificationList.add(0, newMsg);
            filteredList.add(0, newMsg);
            updateRecyclerView();
        }
    }

    // ------------------- Filter notifications -------------------
    private void filterNotifications(String query) {
        filteredList.clear();
        if (TextUtils.isEmpty(query)) {
            filteredList.addAll(notificationList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (NotificationModel10below model : notificationList) {
                if (model.getSender().toLowerCase().contains(lowerQuery)
                        || model.getMessage().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(model);
                }
            }
        }
        Collections.sort(filteredList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        updateRecyclerView();
    }

    // ------------------- Update RecyclerView -------------------
    private void updateRecyclerView() {
        if (filteredList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            lottieEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
            lottieEmptyState.setVisibility(View.GONE);

            if (adapter == null) {
                adapter = new NotificationAdapter10below(filteredList);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateList(filteredList);
            }
        }
    }

    // ------------------- Swipe to delete -------------------
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                NotificationModel10below deleted = filteredList.get(pos);
                dbHelper.deleteNotificationById(deleted.getId());
                filteredList.remove(pos);
                notificationList.remove(deleted);
                adapter.notifyItemRemoved(pos);
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }


}
