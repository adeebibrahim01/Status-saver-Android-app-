package com.mariaxcodexpert.whatsdownloadplus.ui.language;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.mariaxcodexpert.whatsdownloadplus.R;

import java.util.ArrayList;
import java.util.List;

public class LanguageManager {

    private static final String PREF_NAME = "AppSettings";
    private static final String KEY_LANG = "selected_lang";
    private static final String KEY_AUTO_DETECT = "is_auto_detect";
    public static final String DEFAULT_LANG = "en";

    public static String getCurrentActiveLanguage(Context context) {
        return AppCompatDelegate.getApplicationLocales().toLanguageTags();
    }

    public static class LanguageModel {
        private final String code;
        private final String name;

        public LanguageModel(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        @Override public String toString() { return name; }
    }

    public static List<LanguageModel> getSupportedLanguages(@NonNull Context context) {
        List<LanguageModel> langs = new ArrayList<>();
        String[] codes = {"en", "ur", "hi", "ar", "af", "pt", "fr"};
        String[] names = context.getResources().getStringArray(R.array.app_languages);
        for (int i = 0; i < codes.length; i++) {
            langs.add(new LanguageModel(codes[i], names[i]));
        }
        return langs;
    }

    public static void setAutoDetect(Context context, boolean isChecked) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_AUTO_DETECT, isChecked).apply();
    }

    public static boolean isAutoDetectEnabled(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_DETECT, true);
    }
    public static void applyLanguage(@NonNull Context context, @NonNull String langCode) {
        if (langCode.equals("system")) {
            setAutoDetect(context, true);
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            setAutoDetect(context, false);
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LANG, langCode)
                    .apply();

            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode));
        }
    }
    public static String getSavedLanguageCode(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANG, DEFAULT_LANG);
    }

    public static void initAppLanguage(Context context) {
        if (isAutoDetectEnabled(context)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            String lang = getSavedLanguageCode(context);
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
        }
    }
}