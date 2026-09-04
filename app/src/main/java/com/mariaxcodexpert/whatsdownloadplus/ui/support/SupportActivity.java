package com.mariaxcodexpert.whatsdownloadplus.ui.support;

import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;
import com.mariaxcodexpert.whatsdownloadplus.R;
import java.text.SimpleDateFormat;
import java.util.*;

public class SupportActivity extends AppCompatActivity {

    private EditText etMessage;
    private RecyclerView recyclerView;
    private DatabaseReference ticketRef, msgRef;
    private ValueEventListener activeTicketListener, chatMessagesListener;

    private final List<ChatMessage> chatList = new ArrayList<>();
    private ChatAdapter adapter;
    private String deviceId, currentTicketId = null;

    private View welcomeLayout, loadingOverlay, inputArea, closedTicketOptions;
    private TextView tvStatusHeader, tvTicketID;
    private MaterialButton btnReopenTicket, btnNewTicket;
    private ImageView btnHistory;
    private TextView tvConnectionStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        initViews();
        setupFirebase();
        setupChatList();
        checkActiveTicket();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSubmitSupport).setOnClickListener(v -> handleSendMessage());
        btnReopenTicket.setOnClickListener(v -> updateTicketStatus("Open"));
        btnNewTicket.setOnClickListener(v -> resetToNewTicket());
        btnHistory.setOnClickListener(v -> showHistoryDialog());
    }

    private void initViews() {
        etMessage = findViewById(R.id.etSupportMessage);
        recyclerView = findViewById(R.id.chatRecyclerView);
        welcomeLayout = findViewById(R.id.welcomeLayout);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        inputArea = findViewById(R.id.inputArea);
        tvStatusHeader = findViewById(R.id.tvStatusHeader);
        tvTicketID = findViewById(R.id.tvTicketID);
        closedTicketOptions = findViewById(R.id.closedTicketOptions);
        btnReopenTicket = findViewById(R.id.btnReopenTicket);
        btnNewTicket = findViewById(R.id.btnNewTicket);
        btnHistory = findViewById(R.id.btnHistory);
        // Ise initViews() ke andar paste karein
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
    }
    private void setupFirebase() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String uid = user.getUid();
            ticketRef = FirebaseDatabase.getInstance().getReference("Support").child("Tickets").child(uid);
        } else {
            try {
                deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                if (deviceId == null || deviceId.isEmpty()) {
                    deviceId = android.os.Build.MODEL + "_" + android.os.Build.SERIAL;
                }
                ticketRef = FirebaseDatabase.getInstance().getReference("Support").child("Tickets").child(deviceId);
            } catch (Exception e) {
                deviceId = "unknown_device_" + System.currentTimeMillis();
                ticketRef = FirebaseDatabase.getInstance().getReference("Support").child("Tickets").child(deviceId);
            }
        }
    }

    private void setupChatList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ChatAdapter(chatList);
        recyclerView.setAdapter(adapter);
    }

    private void checkActiveTicket() {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);

        if (activeTicketListener != null && ticketRef != null) {
            ticketRef.removeEventListener(activeTicketListener);
        }

        activeTicketListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (!snapshot.exists()) {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    currentTicketId = null;
                    handleTicketUI("New");
                    if (welcomeLayout != null) welcomeLayout.setVisibility(View.VISIBLE);
                    return;
                }

                DataSnapshot lastTicketSnapshot = null;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    lastTicketSnapshot = ds;
                }

                if (lastTicketSnapshot != null) {
                    try {
                        SupportTicket ticket = lastTicketSnapshot.getValue(SupportTicket.class);

                        if (ticket != null && ticket.ticketId != null && !ticket.ticketId.isEmpty()) {
                            currentTicketId = ticket.ticketId;

                            handleTicketUI(ticket.status != null ? ticket.status : "Open");

                            loadMessages(ticket.ticketId);
                        } else {
                            handleTicketUI("New");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    }
                } else {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    handleTicketUI("New");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Firebase error handling
                if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(SupportActivity.this, getString(R.string.error_connection_failed, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        };
        ticketRef.limitToLast(1).addValueEventListener(activeTicketListener);
    }
    private void handleTicketUI(String status) {
        if (currentTicketId != null) {
            tvTicketID.setVisibility(View.VISIBLE);
            String shortId = currentTicketId.length() > 8 ?
                    currentTicketId.substring(currentTicketId.length() - 8) : currentTicketId;
            tvTicketID.setText(getString(R.string.ticket_id_prefix) + shortId.toUpperCase());

            if (getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                ticketRef.child(currentTicketId).child("hasNotification").setValue(false);
            }
        } else {
            tvTicketID.setVisibility(View.GONE);
        }

        if (tvConnectionStatus != null) {
            if ("Closed".equalsIgnoreCase(status)) {
                tvConnectionStatus.setText(getString(R.string.status_ticket_closed));
                tvConnectionStatus.setTextColor(Color.GRAY);

                if (inputArea != null) inputArea.setVisibility(View.GONE);
                if (closedTicketOptions != null) closedTicketOptions.setVisibility(View.VISIBLE);
                if (welcomeLayout != null) welcomeLayout.setVisibility(View.GONE);

            } else if ("Open".equalsIgnoreCase(status)) {
                tvConnectionStatus.setText(getString(R.string.status_connected_support));
                tvConnectionStatus.setTextColor(Color.parseColor("#D4AF37"));

                if (inputArea != null) inputArea.setVisibility(View.VISIBLE);
                if (closedTicketOptions != null) closedTicketOptions.setVisibility(View.GONE);
                if (welcomeLayout != null) welcomeLayout.setVisibility(View.GONE);

            } else {
                tvConnectionStatus.setText(getString(R.string.status_start_conversation));
                tvConnectionStatus.setTextColor(Color.parseColor("#D4AF37"));
                if (inputArea != null) inputArea.setVisibility(View.VISIBLE);
                if (closedTicketOptions != null) closedTicketOptions.setVisibility(View.GONE);
            }
        }
    }

    private void loadMessages(String tId) {
        if (tId == null || tId.isEmpty()) return;

        if (chatMessagesListener != null && msgRef != null) {
            msgRef.removeEventListener(chatMessagesListener);
        }

        msgRef = FirebaseDatabase.getInstance().getReference("Support").child("Messages").child(tId);
        Query sortedQuery = msgRef.orderByChild("serverTimestamp");

        chatMessagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                chatList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        ChatMessage cm = ds.getValue(ChatMessage.class);
                        if (cm != null) chatList.add(cm);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                    if (!chatList.isEmpty() && recyclerView != null) {
                        recyclerView.postDelayed(() ->
                                recyclerView.smoothScrollToPosition(chatList.size() - 1), 100);
                    }
                }
                if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
            }
        };

        sortedQuery.addValueEventListener(chatMessagesListener);
    }
    private void handleSendMessage() {
        String msg = etMessage.getText().toString().trim();
        if (msg.isEmpty()) return;

        if (currentTicketId == null) {
            createNewTicket(msg);
        } else {
            sendMessageToFirebase(msg);
        }
    }

    private void sendAutoAdminReply(String adminMsg) {
        if (currentTicketId == null) return;

        try {
            DatabaseReference autoMsgRef = FirebaseDatabase.getInstance()
                    .getReference("Support").child("Messages").child(currentTicketId);

            String mId = autoMsgRef.push().getKey();
            if (mId == null) return;
         long preciseTime = System.currentTimeMillis();

            String timeString = "UTC_TIMESTAMP";

            ChatMessage message = new ChatMessage(mId, adminMsg, "auto_admin", timeString, true, preciseTime);

            autoMsgRef.child(mId).setValue(message);
        } catch (Exception e) {
            Log.e("SupportSystem", "Crash Prevented in AutoReply: " + e.getMessage());
        }
    }

    private void createNewTicket(String firstMsg) {
        if (ticketRef == null) return;

        String tId = ticketRef.push().getKey();
        if (tId == null) return;

        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        long creationTime = System.currentTimeMillis();
        String time = String.valueOf(creationTime);

        SupportTicket newTicket = new SupportTicket(tId, "Support Request", "Open", firstMsg, time);
        newTicket.hasNotification = false;

        ticketRef.child(tId).setValue(newTicket).addOnSuccessListener(aVoid -> {
            currentTicketId = tId;
            msgRef = FirebaseDatabase.getInstance().getReference("Support").child("Messages").child(tId);
            sendMessageToFirebase(firstMsg);

            String shortId = tId.length() > 6 ? tId.substring(tId.length() - 6).toUpperCase() : tId.toUpperCase();
            sendAutoAdminReply(getString(R.string.auto_admin_initial_reply, shortId));

            if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
        });
    }

    private void sendMessageToFirebase(String msg) {
        if (currentTicketId == null || msgRef == null) return;

        long preciseTime = System.currentTimeMillis();
        String mId = msgRef.push().getKey();

        if (mId != null) {
            ChatMessage message = new ChatMessage(mId, msg, "user", "UTC", true, preciseTime);
            msgRef.child(mId).setValue(message).addOnSuccessListener(aVoid -> {
                if (etMessage != null) etMessage.setText("");
                ticketRef.child(currentTicketId).child("lastMessage").setValue(msg);
            });
        }
    }

    private void updateTicketStatus(String status) {
        if (currentTicketId != null && ticketRef != null) {
            ticketRef.child(currentTicketId).child("status").setValue(status)
                    .addOnSuccessListener(aVoid -> {
                        if ("Open".equalsIgnoreCase(status)) {
                            ticketRef.child(currentTicketId).child("hasNotification").setValue(false);
                            Toast.makeText(this, getString(R.string.toast_ticket_reopened), Toast.LENGTH_SHORT).show();
                            sendAutoAdminReply(getString(R.string.auto_admin_reopen_reply));
                            if (recyclerView != null) {
                                recyclerView.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.toast_ticket_closed_feedback), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, getString(R.string.error_status_update_failed), Toast.LENGTH_SHORT).show());
        }
    }

    private void resetToNewTicket() {
        currentTicketId = null;
        chatList.clear();
        adapter.notifyDataSetChanged();
        handleTicketUI("New");
    }

    private void showHistoryDialog() {
        if (isFinishing()) return;
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);

        ticketRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                if (isFinishing() || isDestroyed()) return;
                if (!snapshot.exists()) {
                    Toast.makeText(SupportActivity.this, getString(R.string.toast_no_tickets_found), Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> itemsList = new ArrayList<>();
                List<SupportTicket> ticketObjects = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        SupportTicket st = ds.getValue(SupportTicket.class);
                        if (st != null) {
                            ticketObjects.add(st);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Collections.reverse(ticketObjects);

                for (SupportTicket st : ticketObjects) {
                    String displayTitle = st.getFormattedDate() + " - " + st.status.toUpperCase();
                    itemsList.add(displayTitle);
                }
                new AlertDialog.Builder(SupportActivity.this, R.style.LuxuryDialogTheme)
                        .setTitle(getString(R.string.dialog_history_title))
                        .setItems(itemsList.toArray(new String[0]), (dialog, which) -> {
                            SupportTicket selected = ticketObjects.get(which);
                            currentTicketId = selected.ticketId;
                            handleTicketUI(selected.status);
                            loadMessages(currentTicketId);
                            if (recyclerView != null) {
                                recyclerView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_btn_close), null)
                        .show();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(SupportActivity.this, getString(R.string.error_database_failed, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    protected void onDestroy() {
        if (activeTicketListener != null) ticketRef.removeEventListener(activeTicketListener);
        if (chatMessagesListener != null && msgRef != null) msgRef.removeEventListener(chatMessagesListener);
        super.onDestroy();
    }

    class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<ChatMessage> list;
        public ChatAdapter(List<ChatMessage> list) { this.list = list; }

        @Override
        public int getItemViewType(int position) {
            return list.get(position).isAdminMessage() ? 2 : 1;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = (viewType == 1) ? R.layout.item_chat_user : R.layout.item_chat_admin;
            return new ChatViewHolder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((ChatViewHolder) holder).bind(list.get(position));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView tvMsg, tvTime, tvRoleName;

            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMsg = itemView.findViewById(R.id.tvMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvRoleName = itemView.findViewById(R.id.tv_role_name);
            }

            void bind(ChatMessage m) {
                tvMsg.setText(m.message);
                tvTime.setText(m.getDisplayTime());
                if (tvRoleName != null) {
                    if ("auto_admin".equalsIgnoreCase(m.role)) {
                        tvRoleName.setText(itemView.getContext().getString(R.string.identity_admin_system));
                    } else if (m.isAdminMessage()) {
                        tvRoleName.setText(itemView.getContext().getString(R.string.identity_admin_adeeb));
                    }
                }
            }
        }
    }
}