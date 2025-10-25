package com.ai_autocreate.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.ai_autocreate.R;
import com.ai_autocreate.activities.MainActivity;
import com.ai_autocreate.services.ProcessingService;

public class NotificationUtils {
    private Context context;
    private NotificationManager notificationManager;

    public NotificationUtils(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channels
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Processing channel
            NotificationChannel processingChannel = new NotificationChannel(
                "processing_channel",
                context.getString(R.string.processing_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
            processingChannel.setDescription(context.getString(R.string.processing_channel_description));
            processingChannel.enableLights(true);
            processingChannel.enableVibration(true);
            notificationManager.createNotificationChannel(processingChannel);

            // Stage updates channel
            NotificationChannel stageUpdatesChannel = new NotificationChannel(
                "stage_updates_channel",
                context.getString(R.string.stage_updates_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
            stageUpdatesChannel.setDescription(context.getString(R.string.stage_updates_channel_description));
            stageUpdatesChannel.enableLights(false);
            stageUpdatesChannel.enableVibration(false);
            notificationManager.createNotificationChannel(stageUpdatesChannel);

            // Errors channel
            NotificationChannel errorsChannel = new NotificationChannel(
                "errors_channel",
                context.getString(R.string.errors_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
            errorsChannel.setDescription(context.getString(R.string.errors_channel_description));
            errorsChannel.enableLights(true);
            errorsChannel.enableVibration(true);
            notificationManager.createNotificationChannel(errorsChannel);

            // Completed channel
            NotificationChannel completedChannel = new NotificationChannel(
                "completed_channel",
                context.getString(R.string.completed_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
            completedChannel.setDescription(context.getString(R.string.completed_channel_description));
            completedChannel.enableLights(true);
            completedChannel.enableVibration(false);
            notificationManager.createNotificationChannel(completedChannel);
        }
    }

    public void showProcessingNotification(String title, String message, int progress, String projectId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("project_id", projectId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create actions for pause and cancel
        Intent pauseIntent = new Intent(context, ProcessingService.class);
        pauseIntent.setAction("PAUSE_PROCESSING");
        pauseIntent.putExtra("project_id", projectId);
        PendingIntent pausePendingIntent = PendingIntent.getService(context, 0, pauseIntent, 0);

        Intent cancelIntent = new Intent(context, ProcessingService.class);
        cancelIntent.setAction("CANCEL_PROCESSING");
        cancelIntent.putExtra("project_id", projectId);
        PendingIntent cancelPendingIntent = PendingIntent.getService(context, 0, cancelIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "processing_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_pause, context.getString(R.string.pause), pausePendingIntent)
            .addAction(R.drawable.ic_cancel, context.getString(R.string.cancel), cancelPendingIntent);

        if (progress >= 0 && progress < 100) {
            builder.setProgress(100, progress, false);
        } else {
            builder.setProgress(0, 0, false);
        }

        notificationManager.notify(1, builder.build());
    }

    public void showStageUpdateNotification(String title, String message, String projectId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("project_id", projectId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "stage_updates_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(2, builder.build());
    }

    public void showErrorNotification(String title, String message, String projectId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("project_id", projectId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "errors_channel")
            .setSmallIcon(R.drawable.ic_error)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify(3, builder.build());
    }

    public void showCompletedNotification(String title, String message, String projectId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("project_id", projectId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "completed_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(4, builder.build());
    }

    public void cancelNotification(int id) {
        notificationManager.cancel(id);
    }

    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }
}
