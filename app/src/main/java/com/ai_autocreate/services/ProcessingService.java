package com.ai_autocreate.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.activities.MainActivity;
import com.ai_autocreate.agents.OrchestratorAgent;
import com.ai_autocreate.agents.SanityCheckAgent;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.NotificationUtils;
import com.ai_autocreate.utils.StoragePaths;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray; // CHANGED: add missing import

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProcessingService extends Service {
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_SET_VALUE = 3;
    public static final int MSG_UPDATE_PROGRESS = 4;
    public static final int MSG_UPDATE_STATUS = 5;

    public static final String ACTION_PROCESSING_UPDATE = "com.ai_autocreate.PROCESSING_UPDATE";
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_PERCENT = "percent";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_PROJECT_ID = "project_id";
    public static final String EXTRA_STATUS = "status";

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "processing_channel";

    private NotificationManager notificationManager;
    private NotificationUtils notificationUtils;
    private JSONLogger logger;
    private OrchestratorAgent orchestratorAgent;
    private SanityCheckAgent sanityCheckAgent;
    private boolean isProcessing = false;
    private String currentProjectId;
    private Messenger messenger;

    // Handler for incoming messages from clients
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    messenger = msg.replyTo;
                    break;
                case MSG_UNREGISTER_CLIENT:
                    messenger = null;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationUtils = new NotificationUtils(this);
        logger = new JSONLogger(this);
        orchestratorAgent = new OrchestratorAgent(this);
        sanityCheckAgent = new SanityCheckAgent(this);

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.processing_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(getString(R.string.processing_channel_description));
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (action != null) {
                switch (action) {
                    case "START_PROCESSING":
                        String projectId = intent.getStringExtra("project_id");
                        String prompt = intent.getStringExtra("prompt");
                        startProcessing(projectId, prompt);
                        break;
                    case "PAUSE_PROCESSING":
                        pauseProcessing();
                        break;
                    case "CANCEL_PROCESSING":
                        cancelProcessing();
                        break;
                    case "RESUME_PROCESSING":
                        resumeProcessing();
                        break;
                }
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void startProcessing(String projectId, String prompt) {
        if (isProcessing) {
            Toast.makeText(this, R.string.already_processing, Toast.LENGTH_SHORT).show();
            return;
        }

        currentProjectId = projectId;
        isProcessing = true;

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification(getString(R.string.processing_started), 0));

        // Start processing in background thread
        new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create project directory if it doesn't exist
                        File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
                        if (!projectDir.exists()) {
                            projectDir.mkdirs();
                        }

                        // Create subdirectories
                        new File(projectDir, "frames").mkdirs();
                        new File(projectDir, "audio").mkdirs();
                        new File(projectDir, "checkpoints").mkdirs();

                        // Step 1: Run sanity check
                        updateProgress(10, getString(R.string.running_sanity_check));
                        JSONObject sanityResult = sanityCheckAgent.runCheck();

                        if (!sanityResult.optBoolean("passed", false)) {
                            updateStatus("error", sanityResult.optString("message", "Sanity check failed"));
                            return;
                        }

                        // Step 2: Create project configuration
                        updateProgress(20, getString(R.string.creating_project_config));
                        JSONObject projectConfig = new JSONObject();
                        projectConfig.put("project_id", currentProjectId);
                        projectConfig.put("title", prompt.substring(0, Math.min(prompt.length(), 50)));
                        projectConfig.put("prompt", prompt);
                        projectConfig.put("workflowType", "auto_generate");
                        projectConfig.put("created_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));

                        // Save project config
                        File configFile = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId + "/project.json");
                        JSONLogger.writeToFile(configFile, projectConfig.toString());

                        // Step 3: Run orchestrator agent
                        updateProgress(30, getString(R.string.running_orchestrator));
                        JSONObject orchestratorInput = new JSONObject();
                        orchestratorInput.put("project_id", currentProjectId);
                        orchestratorInput.put("prompt", prompt);
                        orchestratorInput.put("project_config", projectConfig);

                        JSONObject orchestratorResult = orchestratorAgent.process(orchestratorInput);

                        if (orchestratorResult == null || !orchestratorResult.optBoolean("success", false)) {
                            String errorMessage = orchestratorResult != null ? 
                                orchestratorResult.optString("message", "Orchestrator failed") : 
                                "Orchestrator returned null";
                            updateStatus("error", errorMessage);
                            return;
                        }

                        // Step 4: Generate content based on orchestrator result
                        updateProgress(60, getString(R.string.generating_content));

                        // Simulate content generation with progress updates
                        for (int i = 0; i < 5; i++) {
                            Thread.sleep(1000); // Simulate work
                            updateProgress(60 + (i * 6), getString(R.string.generating_content) + " (" + (i + 1) + "/5)");
                        }

                        // Create placeholder files to indicate successful generation
                        File scriptFile = new File(projectDir, "script.txt");
                        JSONLogger.writeToFile(scriptFile, "Generated script content based on: " + prompt);

                        // Step 5: Finalize project
                        updateProgress(90, getString(R.string.finalizing_project));

                        // Update project config with results
                        projectConfig.put("status", "completed");
                        projectConfig.put("completed_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));

                        JSONLogger.writeToFile(configFile, projectConfig.toString());

                        updateProgress(100, getString(R.string.processing_completed));
                        updateStatus("completed", getString(R.string.processing_completed_successfully));

                    } catch (Exception e) {
                        logger.log("ProcessingService", "Error in processing: " + e.getMessage());
                        updateStatus("error", "Error in processing: " + e.getMessage());
                    } finally {
                        isProcessing = false;
                        stopForeground(true);
                        stopSelf();
                    }
                }
            }).start();
    }

    private void pauseProcessing() {
        // Save checkpoint
        saveCheckpoint();

        // Update notification
        Notification notification = createNotification(getString(R.string.processing_paused), 0);
        notificationManager.notify(NOTIFICATION_ID, notification);

        // Send broadcast
        Intent intent = new Intent(ACTION_PROCESSING_UPDATE);
        intent.putExtra(EXTRA_TYPE, "status");
        intent.putExtra(EXTRA_STATUS, "paused");
        intent.putExtra(EXTRA_PROJECT_ID, currentProjectId);
        sendBroadcast(intent);
    }

    private void cancelProcessing() {
        isProcessing = false;

        // Update notification
        Notification notification = createNotification(getString(R.string.processing_cancelled), 0);
        notificationManager.notify(NOTIFICATION_ID, notification);

        // Send broadcast
        Intent intent = new Intent(ACTION_PROCESSING_UPDATE);
        intent.putExtra(EXTRA_TYPE, "status");
        intent.putExtra(EXTRA_STATUS, "cancelled");
        intent.putExtra(EXTRA_PROJECT_ID, currentProjectId);
        sendBroadcast(intent);

        // Stop service
        stopForeground(true);
        stopSelf();
    }

    private void resumeProcessing() {
        // Load checkpoint and resume processing
        // This is a simplified version - in the real implementation, 
        // this would load the checkpoint and resume from where it left off

        // Update notification
        Notification notification = createNotification(getString(R.string.processing_resumed), 0);
        notificationManager.notify(NOTIFICATION_ID, notification);

        // Send broadcast
        Intent intent = new Intent(ACTION_PROCESSING_UPDATE);
        intent.putExtra(EXTRA_TYPE, "status");
        intent.putExtra(EXTRA_STATUS, "resumed");
        intent.putExtra(EXTRA_PROJECT_ID, currentProjectId);
        sendBroadcast(intent);
    }

    private void saveCheckpoint() {
        try {
            JSONObject checkpoint = new JSONObject();
            checkpoint.put("project_id", currentProjectId);
            checkpoint.put("timestamp", System.currentTimeMillis());
            checkpoint.put("status", "paused");

            File checkpointFile = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId + "/checkpoints/checkpoint.json");
            JSONLogger.writeToFile(checkpointFile, checkpoint.toString());
        } catch (JSONException e) {
            logger.log("ProcessingService", "Error saving checkpoint: " + e.getMessage());
        }
    }

    private void updateProgress(int percent, String message) {
        // Update notification
        Notification notification = createNotification(message, percent);
        notificationManager.notify(NOTIFICATION_ID, notification);

        // Send broadcast
        Intent intent = new Intent(ACTION_PROCESSING_UPDATE);
        intent.putExtra(EXTRA_TYPE, "progress");
        intent.putExtra(EXTRA_PERCENT, percent);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_PROJECT_ID, currentProjectId);
        sendBroadcast(intent);

        // Send message to registered client
        if (messenger != null) {
            try {
                Message msg = Message.obtain(null, MSG_UPDATE_PROGRESS);
                Bundle bundle = new Bundle();
                bundle.putInt(EXTRA_PERCENT, percent);
                bundle.putString(EXTRA_MESSAGE, message);
                bundle.putString(EXTRA_PROJECT_ID, currentProjectId);
                msg.setData(bundle);
                messenger.send(msg);
            } catch (Exception e) {
                // Client is no longer available
                messenger = null;
            }
        }
    }

    private void updateStatus(String status, String message) {
        // Send broadcast
        Intent intent = new Intent(ACTION_PROCESSING_UPDATE);
        intent.putExtra(EXTRA_TYPE, "status");
        intent.putExtra(EXTRA_STATUS, status);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_PROJECT_ID, currentProjectId);
        sendBroadcast(intent);

        // Send message to registered client
        if (messenger != null) {
            try {
                Message msg = Message.obtain(null, MSG_UPDATE_STATUS);
                Bundle bundle = new Bundle();
                bundle.putString(EXTRA_STATUS, status);
                bundle.putString(EXTRA_MESSAGE, message);
                bundle.putString(EXTRA_PROJECT_ID, currentProjectId);
                msg.setData(bundle);
                messenger.send(msg);
            } catch (Exception e) {
                // Client is no longer available
                messenger = null;
            }
        }
    }

    private Notification createNotification(String message, int progress) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        // Create actions for pause and cancel
        Intent pauseIntent = new Intent(this, ProcessingService.class);
        pauseIntent.setAction("PAUSE_PROCESSING");
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, 0);

        Intent cancelIntent = new Intent(this, ProcessingService.class);
        cancelIntent.setAction("CANCEL_PROCESSING");
        PendingIntent cancelPendingIntent = PendingIntent.getService(this, 0, cancelIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_pause, getString(R.string.pause), pausePendingIntent)
            .addAction(R.drawable.ic_cancel, getString(R.string.cancel), cancelPendingIntent);

        if (progress > 0 && progress < 100) {
            builder.setProgress(100, progress, false);
        } else {
            builder.setProgress(0, 0, false);
        }

        return builder.build();
    }
}
