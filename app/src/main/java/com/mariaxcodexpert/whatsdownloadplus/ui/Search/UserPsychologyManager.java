package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.content.Context;
import android.content.SharedPreferences;
import com.mariaxcodexpert.whatsdownloadplus.R;
import java.util.*;

public class UserPsychologyManager {

    private static final String PREF_NAME = "Global_AI_Brain_V3";
    private static final String TIME_PREFIX = "_time_";
    private static final String LINK_PREFIX = "_link_";

    private SharedPreferences prefs;
    private Context context;
    private static final String DEFAULT_FALLBACK = "Luxury Car";

    public UserPsychologyManager(Context context) {
        if (context == null) return;
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private String getTimeMood() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (context == null) return "Night";
        if (hour < 6) return context.getString(R.string.mood_night);
        if (hour < 12) return context.getString(R.string.mood_morning);
        if (hour < 18) return context.getString(R.string.mood_day);
        return context.getString(R.string.mood_evening);
    }

    public void trackSearch(String query) {
        if (query == null || query.trim().isEmpty() || query.length() < 3 || prefs == null) return;
        if (prefs.getAll().size() > 500) clear();

        try {
            String[] words = query.toLowerCase().trim().split("\\s+");
            SharedPreferences.Editor editor = prefs.edit();
            long now = System.currentTimeMillis();

            for (String word : words) {
                if (word == null || word.length() < 3) continue;
                int currentFreq = prefs.getInt(word, 0);
                editor.putInt(word, currentFreq + 1);
                editor.putLong(TIME_PREFIX + word, now);

                for (String other : words) {
                    if (other != null && !word.equals(other) && other.length() > 2) {
                        String key = LINK_PREFIX + word + "_" + other;
                        editor.putInt(key, prefs.getInt(key, 0) + 1);
                    }
                }
            }
            editor.apply();
        } catch (Exception e) {
            android.util.Log.e("AI_BRAIN", "Error tracking search: " + e.getMessage());
        }
    }

    private String getBestKeyword() {
        if (prefs == null) return null;
        Map<String, ?> all = prefs.getAll();
        if (all.isEmpty()) return null;

        long now = System.currentTimeMillis();
        double bestScore = -1;
        String best = null;

        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.startsWith("_")) continue;

            try {
                int freq = (Integer) entry.getValue();
                long time = prefs.getLong(TIME_PREFIX + key, now);
                double hours = Math.max(0.1, (double)(now - time) / (1000.0 * 60 * 60));
                double score = (freq * 0.6) + ((1.0 / (1.0 + hours)) * 4.0);

                if (score > bestScore) {
                    bestScore = score;
                    best = key;
                }
            } catch (Exception e) { continue; }
        }
        return best;
    }

    public String getAIPredictedQuery() {
        return generateBaseQuery();
    }

    private String generateBaseQuery() {
        try {
            String keyword = getBestKeyword();
            String mood = getTimeMood();
            if (keyword == null) {
                return (DEFAULT_FALLBACK + " " + mood).trim();
            }
            double r = Math.random();
            if (r < 0.6) return (keyword + " " + mood).trim();
            else return (DEFAULT_FALLBACK + " " + keyword).trim();

        } catch (Exception e) {
            return DEFAULT_FALLBACK + " " + getTimeMood();
        }
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}