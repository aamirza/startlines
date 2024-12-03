/* This is to ensure Startlines continues running even in the background or on reboot */

package com.asmirza.startlines;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;


public class StartlineService extends Service {
    private static final String CHANNEL_ID = "startline_channel";

    private static final int NOTIFICATION_ID = 1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped.
        Log.d("StartlineService", "Startline service entered");
        createNotificationChannel();
        // TODO: Fix this for Android SDK 34+ (Android 14+)
        startForeground(NOTIFICATION_ID, getNotification());
        Log.d("StartlineService", "Startline foreground service started");

        if (intent != null) {
            String lineType = intent.getStringExtra("lineType");
            Log.d("StartlineService", "StartlineService entered with lineType: " + lineType);

            if (lineType != null) {
                if (lineType.equals("midnight")) {
                    StartlinesManager.scheduleStartlines(this);
                } else if (lineType.equals("startline") || lineType.equals("funline")) {
                    StartlinesManager.executeStartlines(this, lineType);
                }
            }
        }

        //scheduleChecker(15);
        return START_STICKY;
    }

    private void createNotificationChannel() {
        CharSequence name = "Startline Channel";
        String description = "Channel for Startline Service";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        Log.d("StartlineService", "Startline Notification Channel created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    private Notification getNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "startline_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Startline Service")
                .setContentText("Startlines is running in the background")
                .setPriority(NotificationCompat.PRIORITY_LOW);
        Log.d("StartlineService", "Startline Service Notification created");
        return builder.build();
    }
}
