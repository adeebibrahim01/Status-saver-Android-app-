package com.mariaxcodexpert.whatsdownloadplus.ui.peekmode;

import android.content.Context;
import android.util.Log;

import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.PeekMessageEntity.PeekMessageEntity;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PeekChatInsights {


    public static String getMood(Context context, List<PeekMessageEntity> messageList) {
        final String TAG = "MoodAnalyzer";

        try {
            if (context == null) {
                return "Neutral 😐";
            }

            if (messageList == null || messageList.isEmpty()) {
                return context.getString(R.string.mood_neutral) + " 😐";
            }

            double totalSentiment = 0;
            int start = Math.max(0, messageList.size() - 10);
            int evaluatedCount = 0;

            for (int i = start; i < messageList.size(); i++) {
                try {
                    PeekMessageEntity entity = messageList.get(i);
                    if (entity == null) continue;

                    String msg = entity.messageBody;
                    if (msg == null || msg.trim().isEmpty()) continue;

                    evaluatedCount++;
                    String lowerMsg = msg.toLowerCase();

                    // 1. Universal Emoji & Symbol Sentiment Detection
                    if (lowerMsg.matches(".*[😊😂❤️😍✨🎉👍🔥🥰🥳🙌].*")) {
                        totalSentiment += 1.2;
                    }
                    if (lowerMsg.matches(".*[😠😒😑🙄👎😭😡💔😢📉].*")) {
                        totalSentiment -= 1.2;
                    }

                    // 2. Multi-language Universal Slang / Text Tone Indicators
                    boolean hasLetter = false;
                    char[] chars = msg.toCharArray();
                    for (char c : chars) {
                        if (Character.isLetter(c)) {
                            hasLetter = true;
                            break;
                        }
                    }

                    if (msg.length() <= 2 && !hasLetter) {
                        totalSentiment -= 0.4; // Very short replies like "k", "hmm", "."
                    } else if (msg.endsWith("!")) {
                        totalSentiment += 0.3; // Enthusiastic punctuation
                    } else if (msg.endsWith("?")) {
                        totalSentiment += 0.1;
                    }
                } catch (Exception innerE) {
                    Log.e(TAG, "Error evaluating individual message sentiment: " + innerE.getMessage(), innerE);
                }
            }

            // If no meaningful text was found, return neutral safely
            if (evaluatedCount == 0) {
                return context.getString(R.string.mood_neutral) + " 😐";
            }

            // Calculate average sentiment score based on evaluated messages
            double avgSentiment = totalSentiment / evaluatedCount;

            if (avgSentiment > 0.3) {
                return context.getString(R.string.mood_happy) + " 😊";
            } else if (avgSentiment < -0.3) {
                return context.getString(R.string.mood_distant) + " 😐";
            } else {
                return context.getString(R.string.mood_neutral) + " 😐";
            }

        } catch (Exception e) {
            Log.e(TAG, "Critical error in getMood calculation: " + e.getMessage(), e);
            try {
                if (context != null) {
                    return context.getString(R.string.mood_neutral) + " 😐";
                }
            } catch (Exception fallbackEx) {
                Log.e(TAG, "Fallback resource error: " + fallbackEx.getMessage(), fallbackEx);
            }
            return "Neutral 😐";
        }
    }
    public static String getChatDensity(Context context, List<PeekMessageEntity> messageList) {
        if (messageList == null || messageList.isEmpty()) {
            return context.getString(R.string.type_new_connection) + " 🆕";
        }

        int total = messageList.size();
        if (total < 5) {
            return context.getString(R.string.type_low_profile) + " 🐢";
        }

        long timeDiffDays = getTimeDifferenceInDays(messageList);

        // Advanced logic: If chat happened within less than a day, calculate based on hours
        if (timeDiffDays <= 0) {
            if (total > 40) return context.getString(R.string.type_intense_duo) + " ⚡";
            if (total > 15) return context.getString(R.string.type_heavy_chatter) + " 💬";
            return context.getString(R.string.type_casual) + " 📱";
        }

        double messagesPerDay = (double) total / timeDiffDays;

        // Advanced Density Thresholds
        if (total >= 100 && messagesPerDay >= 15.0) {
            return context.getString(R.string.type_intense_duo) + " ⚡";
        } else if (total >= 40 && messagesPerDay >= 5.0) {
            return context.getString(R.string.type_heavy_chatter) + " 💬";
        } else if (total >= 10 && messagesPerDay >= 1.0) {
            return context.getString(R.string.type_casual) + " 📱";
        } else {
            return context.getString(R.string.type_low_profile) + " 🐢";
        }
    }

    private static long getTimeDifferenceInDays(List<PeekMessageEntity> messageList) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date first = sdf.parse(messageList.get(0).timestamp);
            Date last = sdf.parse(messageList.get(messageList.size() - 1).timestamp);
            return Math.abs(last.getTime() - first.getTime()) / (1000 * 60 * 60 * 24);
        } catch (Exception e) { return 0; }
    }

    public static String getPeakHours(Context context, List<PeekMessageEntity> messageList) {
        if (messageList == null || messageList.size() < 2) {
            return context.getString(R.string.peak_na);
        }

        int[] hours = new int[24];
        SimpleDateFormat[] possibleFormats = {
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH),
                new SimpleDateFormat("hh:mm a", Locale.ENGLISH),
                new SimpleDateFormat("HH:mm", Locale.ENGLISH)
        };

        int validParsedCount = 0;
        for (PeekMessageEntity msg : messageList) {
            if (msg.timestamp == null) continue;
            Date date = null;
            for (SimpleDateFormat sdf : possibleFormats) {
                try {
                    sdf.setLenient(true);
                    date = sdf.parse(msg.timestamp);
                    if (date != null) break;
                } catch (Exception ignored) {}
            }
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
                if (hourOfDay >= 0 && hourOfDay < 24) {
                    hours[hourOfDay]++;
                    validParsedCount++;
                }
            }
        }

        if (validParsedCount == 0) {
            return context.getString(R.string.peak_na);
        }

        int maxH = 0;
        for (int i = 0; i < 24; i++) {
            if (hours[i] > hours[maxH]) {
                maxH = i;
            }
        }

        int displayHour = (maxH == 0 ? 12 : maxH > 12 ? maxH - 12 : maxH);
        String amPm = (maxH >= 12 ? "PM" : "AM");

        try {
            String formatString = context.getString(R.string.peak_format);
            // Ensure format string actually expects integer and string types to avoid format conversion exceptions
            if (formatString.contains("%d") && formatString.contains("%s")) {
                return String.format(Locale.getDefault(), formatString, displayHour, amPm);
            }
        } catch (Exception e) {
            Log.e("PeekChatInsights", "Failed to format peak hours string resource: " + e.getMessage(), e);
        }

        return "Peak: " + displayHour + " " + amPm;
    }
    public static String getFavWord(Context context, List<PeekMessageEntity> messageList) {
        if (messageList == null || messageList.isEmpty()) return context.getString(R.string.fav_na);
        Map<String, Integer> wordMap = new HashMap<>();
        for (PeekMessageEntity msg : messageList) {
            if (msg.messageBody == null) continue;
            StringBuilder sb = new StringBuilder();
            for (char c : msg.messageBody.toLowerCase().toCharArray()) {
                if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) sb.append(c);
            }
            String[] words = sb.toString().split("\\s+");
            for (String w : words) if (w.length() > 3) wordMap.put(w, wordMap.getOrDefault(w, 0) + 1);
        }
        String fav = "None";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : wordMap.entrySet()) {
            if (entry.getValue() > maxCount) { maxCount = entry.getValue(); fav = entry.getKey(); }
        }
        return context.getString(R.string.fav_format, fav.equals("None") ? context.getString(R.string.status_na) : fav);
    }

    public static int getTotalMessages(List<PeekMessageEntity> messageList) { return messageList != null ? messageList.size() : 0; }

    public static int getMediaCapturedCount(List<PeekMessageEntity> messageList) {
        int count = 0;
        if (messageList != null) {
            for (PeekMessageEntity msg : messageList) {
                String body = msg.messageBody != null ? msg.messageBody.toLowerCase() : "";
                if (body.contains(".jpg") || body.contains(".png") || body.contains(".mp4") || body.contains("photo") || body.contains("video")) count++;
            }
        }
        return count;
    }

    public static String getAverageReplyTime(Context context, List<PeekMessageEntity> messageList) {
        if (messageList == null || messageList.size() < 2) return context.getString(R.string.status_na);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        long totalDiff = 0; int count = 0;
        try {
            for (int i = 1; i < messageList.size(); i++) {
                Date d1 = sdf.parse(messageList.get(i - 1).timestamp);
                Date d2 = sdf.parse(messageList.get(i).timestamp);
                if (d1 != null && d2 != null) {
                    long diff = Math.abs(d2.getTime() - d1.getTime());
                    if (diff < 3600000) { totalDiff += diff; count++; }
                }
            }
        } catch (ParseException e) { return context.getString(R.string.status_na); }
        if (count == 0) return context.getString(R.string.status_na);
        return ((totalDiff / count) / 60000 == 0 ? 1 : (totalDiff / count) / 60000) + " min";
    }

    public static String getPersonalityArchetype(Context context, List<PeekMessageEntity> messageList) {
        if (messageList == null || messageList.size() < 5) return context.getString(R.string.archetype_observer);
        int totalLength = 0;
        for (PeekMessageEntity msg : messageList) if (msg.messageBody != null) totalLength += msg.messageBody.length();
        double avgLen = (double) totalLength / messageList.size();
        if (avgLen > 50) return context.getString(R.string.archetype_storyteller);
        if (avgLen < 10) return context.getString(R.string.archetype_short_reply);
        return context.getString(R.string.archetype_balanced);
    }

    public static String getCommunicationVibe(Context context, List<PeekMessageEntity> messageList) {
        if (messageList == null || messageList.isEmpty()) return context.getString(R.string.vibe_neutral);
        int questions = 0, emojis = 0;
        for (PeekMessageEntity msg : messageList) {
            String body = msg.messageBody != null ? msg.messageBody : "";
            if (body.contains("?")) questions++;
            if (body.matches(".*[😊😂❤️😍✨🎉👍].*")) emojis++;
        }
        if (questions > messageList.size() / 2) return context.getString(R.string.vibe_curious);
        if (emojis > messageList.size() / 2) return context.getString(R.string.vibe_friendly);
        return context.getString(R.string.vibe_straightforward);
    }

    public static String getEngagementConsistency(Context context, List<PeekMessageEntity> messageList) {
        if (messageList == null || messageList.size() < 10) return context.getString(R.string.cons_building);
        int firstHalf = 0, secondHalf = 0, mid = messageList.size() / 2;
        for (int i = 0; i < messageList.size(); i++) { if (i < mid) firstHalf++; else secondHalf++; }
        if (secondHalf > firstHalf + 5) return context.getString(R.string.cons_rising);
        if (firstHalf > secondHalf + 5) return context.getString(R.string.cons_declining);
        return context.getString(R.string.cons_consistent);
    }

    public static String getPredictedResponseStatus(Context context, List<PeekMessageEntity> messageList) {
        if (messageList == null || messageList.isEmpty()) return context.getString(R.string.status_ready);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            long totalDiff = 0; int pairs = 0;
            for (int i = 1; i < messageList.size(); i++) {
                Date d1 = sdf.parse(messageList.get(i - 1).timestamp);
                Date d2 = sdf.parse(messageList.get(i).timestamp);
                if (d1 != null && d2 != null) { totalDiff += Math.abs(d2.getTime() - d1.getTime()); pairs++; }
            }
            long avgMinutes = (pairs > 0) ? (totalDiff / pairs) / 60000 : 5;
            Date lastMsg = sdf.parse(messageList.get(messageList.size() - 1).timestamp);
            long timeSinceLastMsg = (System.currentTimeMillis() - lastMsg.getTime()) / 60000;
            if (timeSinceLastMsg <= (avgMinutes + 5)) return context.getString(R.string.status_active, avgMinutes == 0 ? 2 : avgMinutes);
            else if (timeSinceLastMsg <= (avgMinutes * 3)) return context.getString(R.string.status_soon);
            else return context.getString(R.string.status_low);
        } catch (Exception e) { return context.getString(R.string.status_high); }
    }
}