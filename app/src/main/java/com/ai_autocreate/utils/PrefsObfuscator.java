package com.ai_autocreate.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class PrefsObfuscator {
    private static final String PREFS_NAME = "AIAutoCreatePrefs";
    private static final String OBFS_KEY = "AIAutoCreateObfsKey";
    private static final Charset CHARSET = Charset.forName("UTF-8");

    private Context context;
    private SharedPreferences prefs;
    private byte[] obfuscationKey;

    public PrefsObfuscator(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.obfuscationKey = getOrCreateObfuscationKey();
    }

    public void putString(String key, String value) {
        if (value == null) {
            prefs.edit().remove(key).apply();
            return;
        }

        String obfuscatedValue = obfuscate(value);
        prefs.edit().putString(key, obfuscatedValue).apply();
    }

    public String getString(String key, String defaultValue) {
        String obfuscatedValue = prefs.getString(key, null);
        if (obfuscatedValue == null) {
            return defaultValue;
        }

        try {
            return deobfuscate(obfuscatedValue);
        } catch (Exception e) {
            // If deobfuscation fails, return default value
            return defaultValue;
        }
    }

    public void putInt(String key, int value) {
        putString(key, Integer.toString(value));
    }

    public int getInt(String key, int defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void putLong(String key, long value) {
        putString(key, Long.toString(value));
    }

    public long getLong(String key, long defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void putBoolean(String key, boolean value) {
        putString(key, Boolean.toString(value));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    public void putFloat(String key, float value) {
        putString(key, Float.toString(value));
    }

    public float getFloat(String key, float defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public boolean contains(String key) {
        return prefs.contains(key);
    }

    private String obfuscate(String value) {
        try {
            byte[] valueBytes = value.getBytes(CHARSET);
            byte[] obfuscatedBytes = new byte[valueBytes.length];

            for (int i = 0; i < valueBytes.length; i++) {
                obfuscatedBytes[i] = (byte) (valueBytes[i] ^ obfuscationKey[i % obfuscationKey.length]);
            }

            return Base64.encodeToString(obfuscatedBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            // If obfuscation fails, return the original value
            return value;
        }
    }

    private String deobfuscate(String obfuscatedValue) {
        try {
            byte[] obfuscatedBytes = Base64.decode(obfuscatedValue, Base64.NO_WRAP);
            byte[] valueBytes = new byte[obfuscatedBytes.length];

            for (int i = 0; i < obfuscatedBytes.length; i++) {
                valueBytes[i] = (byte) (obfuscatedBytes[i] ^ obfuscationKey[i % obfuscationKey.length]);
            }

            return new String(valueBytes, CHARSET);
        } catch (Exception e) {
            // If deobfuscation fails, return the original value
            return obfuscatedValue;
        }
    }

    private byte[] getOrCreateObfuscationKey() {
        String keyString = prefs.getString(OBFS_KEY, null);

        if (keyString == null) {
            // Generate a new key
            byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            keyString = Base64.encodeToString(key, Base64.NO_WRAP);
            prefs.edit().putString(OBFS_KEY, keyString).apply();
        }

        return Base64.decode(keyString, Base64.NO_WRAP);
    }

    public static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(CHARSET));
            return Base64.encodeToString(hashBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return input;
        }
    }
}
