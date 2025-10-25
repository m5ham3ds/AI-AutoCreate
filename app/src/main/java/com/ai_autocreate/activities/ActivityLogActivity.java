package com.ai_autocreate.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.adapters.ActivityLogAdapter;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.StoragePaths;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityLogActivity extends AppCompatActivity {
    private ListView logListView;
    private Spinner filterSpinner;
    private Button refreshButton;
    private Button clearButton;
    private Button exportButton;
    private ProgressBar progressBar;
    private TextView statusTextView;

    private ActivityLogAdapter logAdapter;
    private List<LogEntry> logList;
    private JSONLogger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_log);

        // Initialize views
        logListView = findViewById(R.id.log_list_view);
        filterSpinner = findViewById(R.id.filter_spinner);
        refreshButton = findViewById(R.id.refresh_button);
        clearButton = findViewById(R.id.clear_button);
        exportButton = findViewById(R.id.export_button);
        progressBar = findViewById(R.id.progress_bar);
        statusTextView = findViewById(R.id.status_text_view);

        // Initialize components
        logger = new JSONLogger(this);
        logList = new ArrayList<>();
        logAdapter = new ActivityLogAdapter(this, logList);

        // Setup views
        logListView.setAdapter(logAdapter);
        setupFilterSpinner();

        // Setup listeners
        setupListeners();

        // Load logs
        loadLogs();
    }

    private void setupFilterSpinner() {
        String[] filters = {"All", "Today", "This Week", "This Month", "Errors Only", "Success Only"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, filters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterLogs();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadLogs();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearLogsDialog();
            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportLogs();
            }
        });

        logListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showLogDetails(position);
            }
        });
    }

    private void loadLogs() {
        new LoadLogsTask().execute();
    }

    private void filterLogs() {
        String selectedFilter = (String) filterSpinner.getSelectedItem();

        List<LogEntry> filteredList = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (LogEntry entry : logList) {
            boolean include = false;

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date entryDate = sdf.parse(entry.timestamp);
                long entryTime = entryDate.getTime();

                long timeDiff = currentTime - entryTime;
                long daysDiff = timeDiff / (24 * 60 * 60 * 1000);

                switch (selectedFilter) {
                    case "All":
                        include = true;
                        break;
                    case "Today":
                        include = daysDiff < 1;
                        break;
                    case "This Week":
                        include = daysDiff < 7;
                        break;
                    case "This Month":
                        include = daysDiff < 30;
                        break;
                    case "Errors Only":
                        include = entry.type.equals("error") || entry.type.equals("failure");
                        break;
                    case "Success Only":
                        include = entry.type.equals("success") || entry.type.equals("completed");
                        break;
                }
            } catch (Exception e) {
                // If we can't parse the date, include it by default
                include = true;
            }

            if (include) {
                filteredList.add(entry);
            }
        }

        // Sort by timestamp (newest first)
        Collections.sort(filteredList, new Comparator<LogEntry>() {
            @Override
            public int compare(LogEntry o1, LogEntry o2) {
                return o2.timestamp.compareTo(o1.timestamp);
            }
        });

        logAdapter.updateList(filteredList);
        statusTextView.setText(filteredList.size() + " log entries found");
    }

    private void showLogDetails(int position) {
        LogEntry entry = logAdapter.getItem(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(entry.title);

        StringBuilder message = new StringBuilder();
        message.append("Type: ").append(entry.type).append("\n");
        message.append("Agent: ").append(entry.agent).append("\n");
        message.append("Timestamp: ").append(entry.timestamp).append("\n");
        message.append("Message: ").append(entry.message).append("\n");

        if (entry.details != null && !entry.details.isEmpty()) {
            message.append("Details: ").append(entry.details);
        }

        builder.setMessage(message.toString());
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    private void showClearLogsDialog() {
        String[] options = {getString(R.string.clear_all_logs), getString(R.string.clear_old_logs), getString(R.string.clear_error_logs)};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.clear_logs)
            .setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: // Clear all logs
                            clearAllLogs();
                            break;
                        case 1: // Clear old logs
                            clearOldLogs();
                            break;
                        case 2: // Clear error logs
                            clearErrorLogs();
                            break;
                    }
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void clearAllLogs() {
        new ClearLogsTask("all").execute();
    }

    private void clearOldLogs() {
        new ClearLogsTask("old").execute();
    }

    private void clearErrorLogs() {
        new ClearLogsTask("errors").execute();
    }

    private void exportLogs() {
        new ExportLogsTask().execute();
    }

    private class LoadLogsTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setText(R.string.loading_logs);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                logList.clear();

                // Load logs from different sources
                loadLogsFromDirectory(StoragePaths.getAgentResultsDir());
                loadLogsFromDirectory(StoragePaths.getConfigDir());
                loadAppLogs();

                return true;
            } catch (Exception e) {
                logger.log("ActivityLog", "Error loading logs: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);

            if (success) {
                filterLogs();
            } else {
                statusTextView.setText(R.string.error_loading_logs);
                Toast.makeText(ActivityLogActivity.this, R.string.error_loading_logs, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadLogsFromDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                loadLogsFromDirectory(file.getAbsolutePath());
            } else if (file.getName().endsWith(".json")) {
                loadLogsFromFile(file);
            } else if (file.getName().endsWith(".jsonl")) {
                loadLogsFromJsonlFile(file);
            }
        }
    }

    private void loadLogsFromFile(File file) {
        try {
            String jsonContent = JSONLogger.readFromFile(file);

            if (file.getName().contains("orchestrator")) {
                JSONObject logObj = new JSONObject(jsonContent);
                LogEntry entry = new LogEntry(
                    "Orchestrator",
                    logObj.optString("agent", "Orchestrator"),
                    logObj.optString("message", ""),
                    logObj.optString("timestamp", ""),
                    logObj.optString("success", "false").equals("true") ? "success" : "error",
                    jsonContent
                );
                logList.add(entry);
            } else if (file.getName().contains("sanity_check")) {
                JSONObject logObj = new JSONObject(jsonContent);
                LogEntry entry = new LogEntry(
                    "Sanity Check",
                    logObj.optString("agent", "SanityCheckAgent"),
                    logObj.optString("message", ""),
                    logObj.optString("timestamp", ""),
                    logObj.optBoolean("passed", false) ? "success" : "error",
                    jsonContent
                );
                logList.add(entry);
            }

        } catch (JSONException e) {
            logger.log("ActivityLog", "Error parsing log file " + file.getName() + ": " + e.getMessage());
        }
    }

    private void loadLogsFromJsonlFile(File file) {
        try {
            String content = JSONLogger.readFromFile(file);
            String[] lines = content.split("\n");

            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    try {
                        JSONObject logObj = new JSONObject(line);
                        LogEntry entry = new LogEntry(
                            logObj.optString("agent", "Unknown"),
                            logObj.optString("agent", "Unknown"),
                            logObj.optString("message", ""),
                            logObj.optString("date", ""),
                            logObj.optString("type", "info"),
                            line
                        );
                        logList.add(entry);
                    } catch (JSONException e) {
                        // Skip invalid lines
                    }
                }
            }

        } catch (Exception e) {
            logger.log("ActivityLog", "Error reading JSONL file " + file.getName() + ": " + e.getMessage());
        }
    }

    private void loadAppLogs() {
        try {
            File logDir = new File(StoragePaths.getAgentResultsDir());
            File[] logFiles = logDir.listFiles();

            if (logFiles != null) {
                for (File file : logFiles) {
                    if (file.getName().startsWith("app_log_")) {
                        loadLogsFromJsonlFile(file);
                    }
                }
            }
        } catch (Exception e) {
            logger.log("ActivityLog", "Error loading app logs: " + e.getMessage());
        }
    }

    private class ClearLogsTask extends AsyncTask<Void, Void, Boolean> {
        private String clearType;

        public ClearLogsTask(String clearType) {
            this.clearType = clearType;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setText(R.string.clearing_logs);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (clearType.equals("all")) {
                    // Clear all log files
                    clearDirectory(StoragePaths.getAgentResultsDir());
                    clearDirectory(StoragePaths.getTempDir());
                } else if (clearType.equals("old")) {
                    // Clear logs older than 30 days
                    long thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000);
                    clearOldFiles(StoragePaths.getAgentResultsDir(), thirtyDaysAgo);
                    clearOldFiles(StoragePaths.getTempDir(), thirtyDaysAgo);
                } else if (clearType.equals("errors")) {
                    // Clear only error logs
                    clearErrorLogs(StoragePaths.getAgentResultsDir());
                }

                return true;
            } catch (Exception e) {
                logger.log("ActivityLog", "Error clearing logs: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);

            if (success) {
                loadLogs(); // Reload logs after clearing
                Toast.makeText(ActivityLogActivity.this, R.string.logs_cleared, Toast.LENGTH_SHORT).show();
            } else {
                statusTextView.setText(R.string.error_clearing_logs);
                Toast.makeText(ActivityLogActivity.this, R.string.error_clearing_logs, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void clearDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                clearDirectory(file.getAbsolutePath());
                file.delete();
            } else {
                file.delete();
            }
        }
    }

    private void clearOldFiles(String directoryPath, long cutoffTime) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                clearOldFiles(file.getAbsolutePath(), cutoffTime);
                if (file.listFiles() == null || file.listFiles().length == 0) {
                    file.delete();
                }
            } else if (file.lastModified() < cutoffTime) {
                file.delete();
            }
        }
    }

    private void clearErrorLogs(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                clearErrorLogs(file.getAbsolutePath());
            } else if (file.getName().contains("error") || file.getName().contains("failed")) {
                file.delete();
            }
        }
    }

    private class ExportLogsTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setText(R.string.exporting_logs);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // Create export data
                JSONArray exportArray = new JSONArray();

                for (LogEntry entry : logList) {
                    JSONObject entryObj = new JSONObject();
                    entryObj.put("title", entry.title);
                    entryObj.put("agent", entry.agent);
                    entryObj.put("message", entry.message);
                    entryObj.put("timestamp", entry.timestamp);
                    entryObj.put("type", entry.type);
                    entryObj.put("details", entry.details);
                    exportArray.put(entryObj);
                }

                // Save to file
                String fileName = "activity_log_export_" + System.currentTimeMillis() + ".json";
                File exportFile = new File(StoragePaths.getConfigDir() + "/" + fileName);
                JSONLogger.writeToFile(exportFile, exportArray.toString());

                return true;
            } catch (Exception e) {
                logger.log("ActivityLog", "Error exporting logs: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);

            if (success) {
                Toast.makeText(ActivityLogActivity.this, R.string.logs_exported, Toast.LENGTH_SHORT).show();
            } else {
                statusTextView.setText(R.string.error_exporting_logs);
                Toast.makeText(ActivityLogActivity.this, R.string.error_exporting_logs, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_refresh) {
            loadLogs();
            return true;
        } else if (id == R.id.action_filter) {
            // Open filter dialog
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class LogEntry {
        public String title;
        public String agent;
        public String message;
        public String timestamp;
        public String type;
        public String details;

        public LogEntry(String title, String agent, String message, String timestamp, String type, String details) {
            this.title = title;
            this.agent = agent;
            this.message = message;
            this.timestamp = timestamp;
            this.type = type;
            this.details = details;
        }
    }
}
