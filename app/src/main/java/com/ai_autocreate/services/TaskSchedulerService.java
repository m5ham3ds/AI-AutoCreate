package com.ai_autocreate.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import com.ai_autocreate.utils.JSONLogger;
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

public class TaskSchedulerService extends Service {
    private static final String TAG = "TaskSchedulerService";
    private JSONLogger logger;
    private AlarmManager alarmManager;

    @Override
    public void onCreate() {
        super.onCreate();
        logger = new JSONLogger(this);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if (action != null) {
            switch (action) {
                case "SCHEDULE_TASKS":
                    scheduleTasks();
                    break;
                case "CANCEL_TASKS":
                    cancelTasks();
                    break;
            }
        }

        return START_STICKY;
    }

    private void scheduleTasks() {
        try {
            File tasksFile = new File(StoragePaths.getConfigDir() + "/background_tasks.json");

            if (tasksFile.exists()) {
                String jsonContent = JSONLogger.readFromFile(tasksFile);
                JSONArray tasksArray = new JSONArray(jsonContent);

                for (int i = 0; i < tasksArray.length(); i++) {
                    JSONObject taskObj = tasksArray.getJSONObject(i);
                    scheduleTask(taskObj);
                }
            }

            logger.log(TAG, "Tasks scheduled successfully");
        } catch (JSONException e) {
            logger.log(TAG, "Error scheduling tasks: " + e.getMessage());
        }
    }

    private void scheduleTask(JSONObject taskObj) {
        try {
            String taskId = taskObj.getString("id");
            String taskType = taskObj.getString("type");
            long interval = taskObj.getLong("interval");
            boolean enabled = taskObj.optBoolean("enabled", true);

            if (!enabled) {
                return;
            }

            Intent intent = new Intent(this, TaskReceiver.class);
            intent.setAction("EXECUTE_TASK");
            intent.putExtra("task_id", taskId);
            intent.putExtra("task_type", taskType);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, taskId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Schedule repeating alarm
            long triggerAtMillis = SystemClock.elapsedRealtime() + interval;
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                interval,
                pendingIntent);

            logger.log(TAG, "Task scheduled: " + taskId + " (" + taskType + ")");

        } catch (JSONException e) {
            logger.log(TAG, "Error scheduling task: " + e.getMessage());
        }
    }

    private void cancelTasks() {
        try {
            File tasksFile = new File(StoragePaths.getConfigDir() + "/background_tasks.json");

            if (tasksFile.exists()) {
                String jsonContent = JSONLogger.readFromFile(tasksFile);
                JSONArray tasksArray = new JSONArray(jsonContent);

                for (int i = 0; i < tasksArray.length(); i++) {
                    JSONObject taskObj = tasksArray.getJSONObject(i);
                    cancelTask(taskObj);
                }
            }

            logger.log(TAG, "All tasks cancelled");
        } catch (JSONException e) {
            logger.log(TAG, "Error cancelling tasks: " + e.getMessage());
        }
    }

    private void cancelTask(JSONObject taskObj) {
        try {
            String taskId = taskObj.getString("id");

            Intent intent = new Intent(this, TaskReceiver.class);
            intent.setAction("EXECUTE_TASK");
            intent.putExtra("task_id", taskId);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, taskId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

            alarmManager.cancel(pendingIntent);

            logger.log(TAG, "Task cancelled: " + taskId);

        } catch (JSONException e) {
            logger.log(TAG, "Error cancelling task: " + e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
