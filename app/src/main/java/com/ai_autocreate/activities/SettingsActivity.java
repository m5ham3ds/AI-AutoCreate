package com.ai_autocreate.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.ai_autocreate.R;
import com.ai_autocreate.utils.LanguageUtils;
import com.ai_autocreate.utils.PrefsObfuscator;
import com.ai_autocreate.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private PrefsObfuscator prefs;
    private LanguageUtils languageUtils;
    private ThemeUtils themeUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new PrefsObfuscator(this);
        languageUtils = new LanguageUtils(this);
        themeUtils = new ThemeUtils(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private PrefsObfuscator prefs;
        private LanguageUtils languageUtils;
        private ThemeUtils themeUtils;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            prefs = new PrefsObfuscator(getActivity());
            languageUtils = new LanguageUtils(getActivity());
            themeUtils = new ThemeUtils(getActivity());

            setupLanguagePreference();
            setupThemePreference();
            setupAPIKeysPreference();
        }

        private void setupLanguagePreference() {
            ListPreference languagePreference = (ListPreference) findPreference("app_language");
            
            if (languagePreference != null) {
                languagePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            String languageCode = (String) newValue;
                            languageUtils.setAppLanguage(languageCode);

                            // Show restart dialog
                            new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.restart_required)
                                .setMessage(R.string.restart_language_message)
                                .setPositiveButton(R.string.restart_now, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Restart app
                                        Intent intent = getActivity().getPackageManager()
                                            .getLaunchIntentForPackage(getActivity().getPackageName());
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                    }
                                })
                                .setNegativeButton(R.string.restart_later, null)
                                .show();

                            return true;
                        }
                    });
            }
        }

        private void setupThemePreference() {
            ListPreference themePreference = (ListPreference) findPreference("app_theme");
            

            if (themePreference != null) {
                themePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            String themeValue = (String) newValue;
                            themeUtils.setAppTheme(themeValue);

                            // Show restart dialog
                            new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.restart_required)
                                .setMessage(R.string.restart_theme_message)
                                .setPositiveButton(R.string.restart_now, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Restart app
                                        Intent intent = getActivity().getPackageManager()
                                            .getLaunchIntentForPackage(getActivity().getPackageName());
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                    }
                                })
                                .setNegativeButton(R.string.restart_later, null)
                                .show();

                            return true;
                        }
                    });
            }
        }

        private void setupAPIKeysPreference() {
            Preference apiKeysPreference = findPreference("api_keys");

            if (apiKeysPreference != null) {
                apiKeysPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            // Navigate to API keys management
                            Intent intent = new Intent(getActivity(), APIKeysActivity.class);
                            startActivity(intent);
                            return true;
                        }
                    });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_reset_settings) {
            // Show reset confirmation dialog
            new AlertDialog.Builder(this)
                .setTitle(R.string.reset_settings)
                .setMessage(R.string.reset_settings_confirmation)
                .setPositiveButton(R.string.reset, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Reset settings to default
                        prefs.clear();

                        // Restart app
                        Intent intent = getPackageManager()
                            .getLaunchIntentForPackage(getPackageName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
