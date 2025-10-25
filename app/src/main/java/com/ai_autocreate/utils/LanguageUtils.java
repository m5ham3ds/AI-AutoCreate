package com.ai_autocreate.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LanguageUtils {
    private Context context;
    private PrefsObfuscator prefs;

    public LanguageUtils(Context context) {
        this.context = context;
        this.prefs = new PrefsObfuscator(context);
    }

    public void setAppLanguage(String languageCode) {
        // Save language preference
        prefs.putString("app_language", languageCode);

        // Update configuration
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    public String getCurrentLanguage() {
        return prefs.getString("app_language", Locale.getDefault().getLanguage());
    }

    public boolean isRTL() {
        String language = getCurrentLanguage();
        return language.equals("ar") || language.equals("he") || language.equals("fa");
    }
}
