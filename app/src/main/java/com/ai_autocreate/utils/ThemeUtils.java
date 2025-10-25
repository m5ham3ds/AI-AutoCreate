package com.ai_autocreate.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

public class ThemeUtils {
    private Context context;
    private PrefsObfuscator prefs;

    public ThemeUtils(Context context) {
        this.context = context;
        this.prefs = new PrefsObfuscator(context);
    }

    public void setAppTheme(String themeValue) {
        // Save theme preference
        prefs.putString("app_theme", themeValue);

        // Apply theme
        if (context instanceof Activity) {
            Activity activity = (Activity) context;

            switch (themeValue) {
                case "light":
                    activity.setTheme(R.style.AppTheme_Light);
                    break;
                case "dark":
                    activity.setTheme(R.style.AppTheme_Dark);
                    break;
                case "system":
                default:
                    if (isSystemDarkMode()) {
                        activity.setTheme(R.style.AppTheme_Dark);
                    } else {
                        activity.setTheme(R.style.AppTheme_Light);
                    }
                    break;
            }
        }
    }

    public String getCurrentTheme() {
        return prefs.getString("app_theme", "system");
    }

    public boolean isDarkMode() {
        String theme = getCurrentTheme();

        if (theme.equals("dark")) {
            return true;
        } else if (theme.equals("light")) {
            return false;
        } else {
            // System default
            return isSystemDarkMode();
        }
    }

    private boolean isSystemDarkMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return (context.getResources().getConfiguration().uiMode & 
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        } else {
            // For older versions, we can't reliably detect system dark mode
            return false;
        }
    }
}
