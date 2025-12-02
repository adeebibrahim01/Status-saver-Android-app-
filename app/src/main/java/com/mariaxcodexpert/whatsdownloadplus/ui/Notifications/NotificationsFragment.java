package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications;

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
import android.widget.Toast;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private EditText searchEditText;

    private NotificationAdapter adapter;
    private NotificationDatabaseHelper dbHelper;
    private final List<NotificationModel> notificationList = new ArrayList<>();
    private final List<NotificationModel> filteredList = new ArrayList<>();
    private LottieAnimationView lottieEmptyState;

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
        dbHelper = new NotificationDatabaseHelper(requireContext());

        loadNotifications();
        setupSwipeToDelete();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadNotifications();
            swipeRefreshLayout.setRefreshing(false);
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotifications(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        // Broadcast receiver for Android 10 below live notifications
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String sender = intent.getStringExtra("sender");
                String message = intent.getStringExtra("message");
                long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());

                if (sender != null && message != null) {
                    boolean inserted = insertAndShowToast(sender, message, timestamp);
                    addOrMergeNotification(sender, message, timestamp, inserted);
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

    private void loadNotifications() {
        notificationList.clear();
        Cursor cursor = dbHelper.getAllNotifications();

        if (cursor != null && cursor.moveToFirst()) {
            Map<String, LinkedHashMap<Long, NotificationModel>> groupedMap = new LinkedHashMap<>();

            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper.COLUMN_ID));
                String sender = cursor.getString(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper.COLUMN_SENDER));
                String message = cursor.getString(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper.COLUMN_MESSAGE));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper.COLUMN_TIMESTAMP));

                if (shouldIgnoreNotification(message)) continue;

                NotificationModel newNotification = new NotificationModel(id, sender, message, timestamp);

                groupedMap.putIfAbsent(sender, new LinkedHashMap<>());
                groupedMap.get(sender).putIfAbsent(id, newNotification);

            } while (cursor.moveToNext());
            cursor.close();

            for (Map.Entry<String, LinkedHashMap<Long, NotificationModel>> entry : groupedMap.entrySet()) {
                String sender = entry.getKey();
                List<NotificationModel> messages = new ArrayList<>(entry.getValue().values());

                messages.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                String displayMessage = messages.size() > 1 ?
                        "(" + messages.size() + " messages)" :
                        messages.get(0).getMessage();

                NotificationModel groupModel = new NotificationModel(sender, displayMessage, messages.get(0).getTimestamp());
                groupModel.setGroupedMessages(messages);

                notificationList.add(groupModel);

                Toast.makeText(requireContext(),
                        sender + ": " + messages.size() + " messages loaded",
                        Toast.LENGTH_SHORT).show();
            }
        }

        notificationList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        filteredList.clear();
        filteredList.addAll(notificationList);
        updateRecyclerView();
    }

    public static boolean shouldIgnoreNotification(String message) {
        if (message == null) return true;
        String lower = message.trim().toLowerCase();
        if (lower.isEmpty()) return true;

        return lower.contains("calling") || lower.contains("ringing") ||
                lower.contains("incoming call") || lower.contains("incoming voice call") ||
                lower.contains("incoming video call") || lower.contains("missed voice call") ||
                lower.contains("missed video call") || lower.contains("voice call") ||
                lower.contains("video call") || lower.contains("ongoing call") ||
                lower.contains("recording audio") || lower.contains("playing audio") ||
                lower.contains("typing") || lower.contains("online") ||
                lower.contains("you were added") || lower.contains("created group") ||
                lower.contains("changed this group's icon") || lower.contains("changed this group's subject") ||
                lower.contains("reacted to your message") || lower.contains("new status") ||
                lower.contains("status update") || lower.contains("backup in progress") ||
                lower.contains("restoring messages") || lower.matches("\\d+ new messages?") ||
                lower.matches("\\d+ messages from \\d+ chats?");
    }

    private void filterNotifications(String query) {
        filteredList.clear();
        if (TextUtils.isEmpty(query)) filteredList.addAll(notificationList);
        else {
            String lowerQuery = query.toLowerCase();
            for (NotificationModel model : notificationList) {
                boolean match = model.getSender().toLowerCase().contains(lowerQuery)
                        || model.getMessage().toLowerCase().contains(lowerQuery);

                if (!match && model.getGroupedMessages() != null) {
                    List<NotificationModel> matchedMessages = new ArrayList<>();
                    for (NotificationModel msg : model.getGroupedMessages())
                        if (msg.getMessage().toLowerCase().contains(lowerQuery)) matchedMessages.add(msg);

                    if (!matchedMessages.isEmpty()) {
                        String displayMessage = matchedMessages.size() > 1 ?
                                "(" + matchedMessages.size() + " messages)" :
                                matchedMessages.get(0).getMessage();
                        NotificationModel filteredGroup = new NotificationModel(
                                model.getSender(),
                                displayMessage,
                                matchedMessages.get(0).getTimestamp()
                        );
                        filteredGroup.setGroupedMessages(matchedMessages);
                        filteredList.add(filteredGroup);
                        continue;
                    }
                }

                if (match) filteredList.add(model);
            }
        }

        filteredList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        updateRecyclerView();
    }

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
                adapter = new NotificationAdapter(filteredList);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateList(filteredList);
            }
        }
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                NotificationModel deleted = filteredList.get(position);

                if (deleted.getGroupedMessages() != null)
                    for (NotificationModel msg : deleted.getGroupedMessages())
                        dbHelper.deleteNotificationById(msg.getId());
                else dbHelper.deleteNotificationById(deleted.getId());

                filteredList.remove(position);
                adapter.notifyItemRemoved(position);
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    private boolean insertAndShowToast(String sender, String message, long timestamp) {
        if (sender == null || message == null) return false;

        // ONLY ignore system/call messages on Android 10+ (do not ignore for Android 10 below)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (shouldIgnoreNotification(message)) {
                Toast.makeText(requireContext(), "Ignored system/call message", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        Cursor cursor = dbHelper.getAllNotifications();
        boolean exists = false;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String dbSender = cursor.getString(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper.COLUMN_SENDER));
                String dbMessage = cursor.getString(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper.COLUMN_MESSAGE));
                long dbTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(NotificationDatabaseHelper.COLUMN_TIMESTAMP));

                if (dbSender.equals(sender) && dbMessage.equals(message) && Math.abs(dbTimestamp - timestamp) < 1000) {
                    exists = true;
                    break;
                }
            }
            cursor.close();
        }

        if (!exists) {
            dbHelper.insertNotification(sender, message, timestamp);
            Toast.makeText(requireContext(),
                    "Inserted: " + sender + " (" + message + ")",
                    Toast.LENGTH_SHORT).show();
            return true;
        } else {
            Toast.makeText(requireContext(), "Duplicate message ignored", Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    private void addOrMergeNotification(String sender, String message, long timestamp, boolean inserted) {
        if (!inserted) return;

        boolean merged = false;
        for (NotificationModel model : notificationList) {
            if (model.getSender().equals(sender)) {
                List<NotificationModel> grouped = model.getGroupedMessages();
                if (grouped == null) grouped = new ArrayList<>();
                NotificationModel newMsg = new NotificationModel(-1, sender, message, timestamp);
                grouped.add(0, newMsg);
                model.setGroupedMessages(grouped);
                model.setMessage(grouped.size() > 1 ? "(" + grouped.size() + " messages)" : message);

                Toast.makeText(requireContext(),
                        sender + ": " + grouped.size() + " messages now",
                        Toast.LENGTH_SHORT).show();

                merged = true;
                break;
            }
        }

        if (!merged) {
            NotificationModel newModel = new NotificationModel(-1, sender, message, timestamp);
            notificationList.add(0, newModel);
            Toast.makeText(requireContext(),
                    sender + ": 1 message now",
                    Toast.LENGTH_SHORT).show();
        }

        filteredList.clear();
        filteredList.addAll(notificationList);
        updateRecyclerView();
    }
}
