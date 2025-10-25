package com.ai_autocreate.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.ai_autocreate.utils.JSONLogger;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        JSONLogger logger = new JSONLogger(context);

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            logger.log(TAG, "Boot completed received");

            // Start task scheduler service
            Intent schedulerIntent = new Intent(context, TaskSchedulerService.class);
            schedulerIntent.setAction("SCHEDULE_TASKS");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(schedulerIntent);
            } else {
                context.startService(schedulerIntent);
            }

            // Start processing service if needed
            Intent processingIntent = new Intent(context, ProcessingService.class);
            // Add any necessary extras for processing

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(processingIntent);
            } else {
                context.startService(processingIntent);
            }
        }
    }
}
