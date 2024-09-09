/* This is to ensure Startlines continues running even in the background or on reboot */

package com.asmirza.startlines;


import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;


public class StartlineService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped.
        Log.d("StartlineService", "Startline service entered");
        startForeground(1, getNotification());
        Log.d("StartlineService", "Startline foreground service started");

        String lineType = intent.getStringExtra("lineType");
        Log.d("StartlineService", "StartlineService entered with lineType: " + lineType);



        //scheduleChecker(15);
        return START_STICKY;
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
