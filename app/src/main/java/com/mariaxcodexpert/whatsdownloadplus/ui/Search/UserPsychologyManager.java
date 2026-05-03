package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class UserPsychologyManager {
    private static final String PREF_NAME = "UserPsychology";
    private SharedPreferences prefs;

    // 🔥 LUXURY DEFAULT TAGS: Ye keywords Pexels se premium cinematic results late hain
    private static final String[] LUXURY_DEFAULTS = {
            "Aesthetic Dark 4k",
            "Cinematic Luxury Life",
            "Deep Ocean Aesthetic",
            "Minimalist Architecture",
            "Supercar Neon Night",
            "Dreamy Sunset 8k",
            "Abstract Golden Glow",
            "Premium Coffee Mood",
            "Urban Street Style",
            "Nature Landscape Cinematic"
    };

    private static final Set<String> STOP_WORDS = new HashSet<String>() {{
        add("how"); add("to"); add("best"); add("top"); add("the"); add("for");
        add("and"); add("with"); add("video"); add("status"); add("download");
    }};

    public UserPsychologyManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void trackSearch(String query) {
        if (query == null || query.isEmpty()) return;
        String[] words = query.toLowerCase().trim().split("\\s+");
        SharedPreferences.Editor editor = prefs.edit();
        for (String word : words) {
            if (word.length() > 2 && !STOP_WORDS.contains(word)) {
                int currentCount = prefs.getInt(word, 0);
                if (currentCount < 50) {
                    editor.putInt(word, currentCount + 1);
                }
            }
        }
        editor.apply();
    }

    public List<String> getTopInterests(int limit) {
        Map<String, ?> allEntries = prefs.getAll();
        List<Map.Entry<String, Integer>> sortedInterests = new ArrayList<>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getValue() instanceof Integer) {
                sortedInterests.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), (Integer) entry.getValue()));
            }
        }
        Collections.sort(sortedInterests, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        List<String> topTags = new ArrayList<>();
        int actualLimit = Math.min(sortedInterests.size(), limit);
        for (int i = 0; i < actualLimit; i++) {
            topTags.add(sortedInterests.get(i).getKey());
        }
        return topTags;
    }

    /**
     * 🔥 ADVANCED LUXURY LOGIC:
     * User ko screen kholte hi "Premium" feel karwane k liye mixed query return kerta h.
     */
    public String getMixedRecommendedQuery(String[] fallbackTags) {
        List<String> topInterests = getTopInterests(3);
        Random random = new Random();

        // 1. Agar user new h, to Luxury Defaults ma se pick krain
        if (topInterests.isEmpty()) {
            return LUXURY_DEFAULTS[random.nextInt(LUXURY_DEFAULTS.length)];
        }

        // 2. ✧ SMART MIXING ✧
        // User k top interest ko "Luxury Modifiers" k sath mix krain taake result 4k aye
        String baseInterest = topInterests.get(0);
        String[] modifiers = {"4k Aesthetic", "Cinematic", "High Resolution", "Dark Moody", "Abstract"};
        String modifier = modifiers[random.nextInt(modifiers.length)];

        // Agar user ka interest "Car" h, to query banegi "Car 4k Aesthetic" -> Jo k stunning dikhega.
        return baseInterest + " " + modifier;
    }

    public void clearPsychology() {
        prefs.edit().clear().apply();
    }
}