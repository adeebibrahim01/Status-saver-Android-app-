package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.*;

public class UserPsychologyManager {

    private static final String PREF_NAME = "Global_AI_Brain_V3";
    private static final String TIME_PREFIX = "_time_";
    private static final String LINK_PREFIX = "_link_"; // co-occurrence

    private SharedPreferences prefs;
    private Context context;

    public UserPsychologyManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 🌍 GLOBAL TRENDS
    private static final Map<String, String[]> GLOBAL_PULSE = new HashMap<String, String[]>() {{
        put("PK", new String[]{"Sufi Art", "Urdu Poetry", "Truck Art", "Northern Pakistan", "Coke Studio"});
        put("DEFAULT", new String[]{"Cyberpunk", "Nature", "Abstract", "Space", "Minimal"});
    }};

    // 🕒 TIME MOOD
    private String getTimeMood() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 6) return "Night";
        if (hour < 12) return "Morning";
        if (hour < 18) return "Day";
        return "Evening";
    }

    // 🌐 SAFE COUNTRY
    private String getCountry() {
        try {
            String c = Locale.getDefault().getCountry();
            return c.isEmpty() ? "DEFAULT" : c;
        } catch (Exception e) {
            return "DEFAULT";
        }
    }

    // 🔗 TRACK SEARCH WITH RELATIONSHIP (CO-OCCURRENCE)
    public void trackSearch(String query) {
        if (query == null || query.length() < 3) return;

        String[] words = query.toLowerCase().split("\\s+");
        SharedPreferences.Editor editor = prefs.edit();
        long now = System.currentTimeMillis();

        for (String word : words) {
            if (word.length() < 3) continue;

            // frequency
            editor.putInt(word, prefs.getInt(word, 0) + 1);
            editor.putLong(TIME_PREFIX + word, now);

            // co-occurrence learning
            for (String other : words) {
                if (!word.equals(other) && other.length() > 2) {
                    String key = LINK_PREFIX + word + "_" + other;
                    editor.putInt(key, prefs.getInt(key, 0) + 1);
                }
            }
        }
        editor.apply();
    }

    // 🧠 SIMILARITY ENGINE (Pseudo Embedding)
    private List<String> getSimilarWords(String baseWord) {
        Map<String, ?> all = prefs.getAll();
        Map<String, Integer> similarityMap = new HashMap<>();

        for (String key : all.keySet()) {
            if (key.startsWith(LINK_PREFIX + baseWord + "_")) {
                String other = key.replace(LINK_PREFIX + baseWord + "_", "");
                int score = prefs.getInt(key, 0);
                similarityMap.put(other, score);
            }
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(similarityMap.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            result.add(list.get(i).getKey());
        }

        return result;
    }

    // 🧠 BEST KEYWORD (Recency + Frequency)
    private String getBestKeyword() {
        Map<String, ?> all = prefs.getAll();
        long now = System.currentTimeMillis();

        double bestScore = 0;
        String best = null;

        for (String key : all.keySet()) {
            if (key.startsWith("_")) continue;

            int freq = prefs.getInt(key, 0);
            long time = prefs.getLong(TIME_PREFIX + key, now);

            double hours = (now - time) / (1000.0 * 60 * 60);
            double recency = 1 / (1 + hours);

            double score = freq * 0.6 + recency * 4;

            if (score > bestScore) {
                bestScore = score;
                best = key;
            }
        }

        return best;
    }

    // 🧠 AUTO CATEGORY LEARNING
    private String buildCategory(String word) {
        List<String> similar = getSimilarWords(word);

        if (similar.isEmpty()) return word;

        // combine similar words → dynamic category
        StringBuilder sb = new StringBuilder();
        sb.append(word);

        for (String s : similar) {
            sb.append(" ").append(s);
        }

        return sb.toString();
    }

    // 🤖 FINAL AI PREDICTION
    public String getAIPredictedQuery() {
        String keyword = getBestKeyword();
        String mood = getTimeMood();
        String country = getCountry();

        String[] trends = GLOBAL_PULSE.getOrDefault(country, GLOBAL_PULSE.get("DEFAULT"));
        String trend = trends[new Random().nextInt(trends.length)];

        if (keyword == null) {
            return trend + " " + mood;
        }

        String smartCluster = buildCategory(keyword);

        double r = Math.random();

        if (r < 0.55) {
            return smartCluster + " " + mood;
        } else if (r < 0.85) {
            return smartCluster + " " + trend;
        } else {
            return trend + " " + mood;
        }
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}