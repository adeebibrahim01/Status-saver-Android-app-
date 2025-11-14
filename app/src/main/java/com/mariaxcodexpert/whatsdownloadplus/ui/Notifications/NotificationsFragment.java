package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications;

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

        // Load notifications initially
        loadNotifications();

        // Swipe-to-delete
        setupSwipeToDelete();

        // Pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadNotifications();
            swipeRefreshLayout.setRefreshing(false);
        });

        // Live search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotifications(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        return view;
    }

    private void loadNotifications() {
        notificationList.clear();
        Cursor cursor = dbHelper.getAllNotifications();

        if (cursor != null && cursor.moveToFirst()) {
            // Map to group notifications by sender
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

            // Flatten grouped messages
            for (Map.Entry<String, LinkedHashMap<Long, NotificationModel>> entry : groupedMap.entrySet()) {
                String sender = entry.getKey();
                List<NotificationModel> messages = new ArrayList<>(entry.getValue().values());

                messages.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                String displayMessage = messages.size() > 1 ? "(" + messages.size() + " messages)" : messages.get(0).getMessage();

                NotificationModel groupModel = new NotificationModel(sender, displayMessage, messages.get(0).getTimestamp());
                groupModel.setGroupedMessages(messages);

                notificationList.add(groupModel);
            }
        }

        // Sort newest -> oldest
        notificationList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        filteredList.clear();
        filteredList.addAll(notificationList);
        updateRecyclerView();
    }

    public static boolean shouldIgnoreNotification(String message) {
        if (message == null) return true;
        String lower = message.trim().toLowerCase();
        if (lower.isEmpty()) return true;

        return
                // 1. CALL RELATED
                lower.contains("calling") || lower.contains("ringing") || lower.contains("incoming call") ||
                        lower.contains("incoming voice call") || lower.contains("incoming video call") ||
                        lower.contains("missed voice call") || lower.contains("missed video call") ||
                        lower.contains("voice call") || lower.contains("video call") ||
                        lower.contains("ongoing call") || lower.contains("call ended") ||
                        lower.contains("call on hold") ||

                        // 2. MEDIA / VOICE NOTES
                        lower.contains("recording audio") || lower.contains("recording…") || lower.contains("recording...") ||
                        lower.contains("playing audio") || lower.contains("listened") ||
                        lower.contains("listening…") || lower.contains("listening...") ||

                        // 3. TYPING / ONLINE
                        lower.contains("typing…") || lower.contains("typing...") || lower.contains("online") ||

                        // 4. GROUP ACTIVITY
                        lower.contains("you were added") || lower.contains("you were removed") || lower.contains("added you") ||
                        lower.contains("created group") || lower.contains("changed this group's icon") ||
                        lower.contains("changed the group description") || lower.contains("changed this group's subject") ||
                        lower.contains("changed group settings") ||

                        // 5. REACTIONS
                        lower.contains("reacted to your message") || lower.contains("reacted ") ||

                        // 6. STATUS
                        lower.contains("new status") || lower.contains("status update") ||
                        lower.contains("new status update") || lower.contains("viewed your status") ||

                        // 7. BACKUP / SYSTEM
                        lower.contains("backup in progress") || lower.contains("restoring messages") ||
                        lower.contains("connecting...") || lower.contains("reconnecting...") ||
                        lower.contains("checking for new messages") ||

                        // 8. MULTI-DEVICE
                        lower.contains("linked device added") || lower.contains("linked device removed") ||
                        lower.contains("syncing messages") || lower.contains("messages may be insecure") ||

                        // 9. SECURITY / BROADCAST
                        lower.contains("your security code has changed") ||
                        lower.contains("messages are now secured with end-to-end encryption") ||

                        // 10. COUNT-ONLY
                        lower.matches("\\d+ new messages?") ||
                        lower.matches("\\d+ messages from \\d+ chats?");
    }

    private void filterNotifications(String query) {
        filteredList.clear();

        if (TextUtils.isEmpty(query)) {
            filteredList.addAll(notificationList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (NotificationModel model : notificationList) {

                boolean match = model.getSender().toLowerCase().contains(lowerQuery)
                        || model.getMessage().toLowerCase().contains(lowerQuery);

                if (!match && model.getGroupedMessages() != null) {
                    List<NotificationModel> matchedMessages = new ArrayList<>();
                    for (NotificationModel msg : model.getGroupedMessages()) {
                        if (msg.getMessage().toLowerCase().contains(lowerQuery)) {
                            matchedMessages.add(msg);
                        }
                    }

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
            lottieEmptyState.setVisibility(View.VISIBLE);  // ✅ Lottie show
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
            lottieEmptyState.setVisibility(View.GONE);     // ✅ Lottie hide

            if (adapter == null) {
                adapter = new NotificationAdapter(filteredList);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateList(filteredList);
            }
        }
    }


    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
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

                if (deleted.getGroupedMessages() != null) {
                    for (NotificationModel msg : deleted.getGroupedMessages()) {
                        dbHelper.deleteNotificationById(msg.getId());
                    }
                } else {
                    dbHelper.deleteNotificationById(deleted.getId());
                }

                filteredList.remove(position);
                adapter.notifyItemRemoved(position);
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }
}
