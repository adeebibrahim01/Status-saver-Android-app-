package com.mariaxcodexpert.whatsdownloadplus.ui.peekmode;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.local.PeekMessageEntity.PeekMessageEntity;

import java.util.List;

public class PeekChatDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peek_chat_detail);

        String senderName = getIntent().getStringExtra("senderName");
        if (senderName == null) {
            finish();
            return;
        }

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            String uid = db.profileDao().getFirstUserUid();
            List<PeekMessageEntity> chatHistory = db.peekDao().getChatHistory(senderName, uid);
            db.peekDao().resetUnreadCount(senderName, uid);

            runOnUiThread(() -> setupUI(chatHistory, senderName));
        }).start();
    }

    private void setupUI(List<PeekMessageEntity> chatHistory, String senderName) {

        TextView tvTitle = findViewById(R.id.tvChatTitle);
        ShapeableImageView imgProfile = findViewById(R.id.imgToolbarProfile);
        TextView txtAlphabet = findViewById(R.id.txtAlphabet);
        tvTitle.setText(senderName);
        txtAlphabet.setText(String.valueOf(senderName.charAt(0)).toUpperCase());
        imgProfile.setVisibility(View.GONE);
        txtAlphabet.setVisibility(View.VISIBLE);

        TextView tvTotalMessages = findViewById(R.id.tvTotalMessages);
        TextView tvAvgReplyTime = findViewById(R.id.tvAvgReplyTime);
        TextView tvMediaCaptured = findViewById(R.id.tvMediaCaptured);

        TextView tvMood = findViewById(R.id.tvMood);
        TextView tvDensity = findViewById(R.id.tvDensity);
        TextView tvPeak = findViewById(R.id.tvPeakHours);
        TextView tvFav = findViewById(R.id.tvFavWord);

        TextView tvPersonality = findViewById(R.id.tvPersonalityType);
        TextView tvCommunication = findViewById(R.id.tvCommunicationStyle);

        if (chatHistory != null && !chatHistory.isEmpty()) {
            tvTotalMessages.setText(String.valueOf(PeekChatInsights.getTotalMessages(chatHistory)));
            tvAvgReplyTime.setText(PeekChatInsights.getAverageReplyTime(this, chatHistory));
            tvMediaCaptured.setText(String.valueOf(PeekChatInsights.getMediaCapturedCount(chatHistory)));

            tvMood.setText(PeekChatInsights.getMood(this, chatHistory));
            tvDensity.setText(PeekChatInsights.getChatDensity(this, chatHistory));
            tvPeak.setText(PeekChatInsights.getPeakHours(this, chatHistory));
            tvFav.setText(PeekChatInsights.getFavWord(this, chatHistory));

            tvPersonality.setText(getString(R.string.label_type, PeekChatInsights.getPersonalityArchetype(this, chatHistory)));
            tvCommunication.setText(getString(R.string.label_vibe, PeekChatInsights.getCommunicationVibe(this, chatHistory)));

            String moodText = PeekChatInsights.getMood(this, chatHistory);
            String consistency = PeekChatInsights.getEngagementConsistency(this, chatHistory);
            tvMood.setText(moodText + getString(R.string.label_trend, consistency));


        }

        RecyclerView rv = findViewById(R.id.rvDetailMessages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new PeekChatDetailAdapter(chatHistory));
        if (chatHistory != null && !chatHistory.isEmpty()) rv.scrollToPosition(chatHistory.size() - 1);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }
}