package com.ai_autocreate.utils;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JSONLogger {
    private Context context;
    private String logDir;

    public JSONLogger(Context context) {
        this.context = context;
        this.logDir = StoragePaths.getAgentResultsDir();

        // Ensure log directory exists
        File dir = new File(logDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void log(String agent, String message) {
        try {
            JSONObject logEntry = new JSONObject();
            logEntry.put("agent", agent);
            logEntry.put("message", message);
            logEntry.put("timestamp", System.currentTimeMillis());
            logEntry.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            writeLogEntry(logEntry.toString());
        } catch (JSONException e) {
            // If we can't create JSON, just write the message as plain text
            writeLogEntry("{\"agent\":\"" + agent + "\",\"message\":\"" + message + "\",\"timestamp\":" + System.currentTimeMillis() + "}");
        }
    }

    public void log(String agent, JSONObject data) {
        try {
            JSONObject logEntry = new JSONObject();
            logEntry.put("agent", agent);
            logEntry.put("data", data);
            logEntry.put("timestamp", System.currentTimeMillis());
            logEntry.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            writeLogEntry(logEntry.toString());
        } catch (JSONException e) {
            // If we can't create JSON, just write the message as plain text
            log(agent, "Error logging JSON data: " + e.getMessage());
        }
    }

    private void writeLogEntry(String logEntry) {
        try {
            // Create log file for today
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            File logFile = new File(logDir + "/app_log_" + today + ".jsonl");

            // Append to log file
            synchronized (this) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
                writer.write(logEntry);
                writer.newLine();
                writer.close();
            }
        } catch (IOException e) {
            // If we can't write to file, try to write to a fallback location
            try {
                File fallbackFile = new File(context.getExternalFilesDir(null), "fallback_log.jsonl");
                BufferedWriter writer = new BufferedWriter(new FileWriter(fallbackFile, true));
                writer.write(logEntry);
                writer.newLine();
                writer.close();
            } catch (IOException ex) {
                // If even the fallback fails, there's not much we can do
                ex.printStackTrace();
            }
        }
    }

    public static void writeToFile(File file, String content) {
        try {
            // Ensure parent directory exists
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // Write content to file
            synchronized (file.getAbsolutePath().intern()) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write(content);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readFromFile(File file) {
        try {
            if (!file.exists()) {
                return "";
            }

            StringBuilder content = new StringBuilder();
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line);
                content.append(System.getProperty("line.separator"));
            }

            reader.close();
            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
