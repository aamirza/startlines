package com.asmirza.startlines;


import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.time.LocalDate;

public class NotificationHelper {

    private static final String CHANNEL_ID = "startlines_notifications";
    private static final int PERMANENT_NOTIFICATION_ID = 100;
    private static final int X_MODE_NOTIFICATION_ID = 200;
    private static final int TIMER_NOTIFICATION_ID = 300;
    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 15;
    private static final int START_STARTLINE_TIMER_REQUEST_CODE = 10;
    private static final int START_FUNLINE_TIMER_REQUEST_CODE = 11;
    private static final int START_TIMER_REQUEST_CODE = 12;
    private static final int SNOOZE_REQUEST_CODE = 13;
    private static final int STOP_TIMER_REQUEST_CODE = 14;
    private static final int START_BREAK_TIMER_REQUEST_CODE = 16;




    public static void createNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Startlines Action Notifications",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notifications with buttons for the Startlines app");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            Log.d("NotificationHelper", "Startlines Action Notification channel created");
        }
    }

    private static PendingIntent getOpenAppPendingIntent(Context context) {
        Intent openAppIntent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static void showPermanentNotification(Context context, String startlineStatus, String funlineStatus) {
        /*  A permanent notification with the status of Startline and Funline, where the user can
        **  press "Start Startline Timer" or "Start Funline Timer" as action buttons. */
        PendingIntent openAppPendingIntent = getOpenAppPendingIntent(context);
        Intent startStartlineIntent = new Intent(context, MainActivity.class);
        startStartlineIntent.setAction("ACTION_START_STARTLINE_TIMER");
        PendingIntent startStartlinePendingIntent = PendingIntent.getActivity(
                context,
                START_STARTLINE_TIMER_REQUEST_CODE,
                startStartlineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent startFunlineIntent = new Intent(context, MainActivity.class);
        startFunlineIntent.setAction("ACTION_START_FUNLINE_TIMER");
        PendingIntent startFunlinePendingIntent = PendingIntent.getActivity(
                context,
                START_FUNLINE_TIMER_REQUEST_CODE,
                startFunlineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Startlines Status")
                .setContentText("Startline: " + startlineStatus + ", Funline: " + funlineStatus)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openAppPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Start Startline Timer", startStartlinePendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Start Funline Timer", startFunlinePendingIntent)
                .extend(new NotificationCompat.WearableExtender());


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w("NotificationHelper", "Notification permission not granted");
            return; // Don't proceed if permission is not granted
        }

        Log.d("NotificationHelper", "Showing/updated permanent notification");
        NotificationManagerCompat.from(context).notify(PERMANENT_NOTIFICATION_ID, builder.build());
    }

    public static void showXModeNotification(Context context, boolean isStartline) {
        // Example for X Mode
        /* When Startline or Funline hit X mode and no timer is running, another different
        ** notification is sent. This has a "Start" and "Snooze" button. */
        PendingIntent openAppPendingIntent = getOpenAppPendingIntent(context);
        Intent startIntent = new Intent(context, MainActivity.class);
        startIntent.setAction("ACTION_START_TIMER");
        PendingIntent startPendingIntent = PendingIntent.getActivity(
                context,
                START_TIMER_REQUEST_CODE,
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent snoozeIntent = new Intent(context, MainActivity.class);
        snoozeIntent.setAction("ACTION_SNOOZE");
        PendingIntent snoozePendingIntent = PendingIntent.getActivity(
                context,
                SNOOZE_REQUEST_CODE,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Startline in X Mode")
                .setContentText("Startline or Funline needs your attention!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openAppPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Start", startPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Snooze", snoozePendingIntent);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w("NotificationHelper", "Notification permission not granted");
            return; // Don't proceed if permission is not granted
        }


        NotificationManagerCompat.from(context).notify(X_MODE_NOTIFICATION_ID, builder.build());
    }

    public static void showTimerNotification(Context context, int minutesRunning, String workingUntil, double percentCompliant, double potentialCompliant, boolean isStartline) {
        showTimerNotification(context, String.valueOf(minutesRunning), workingUntil, percentCompliant, potentialCompliant, isStartline);
    }

    public static void showTimerNotification(Context context, String minutesRunning, String workingUntil, double percentCompliant, double potentialCompliant, boolean isStartline) {
        // Example for Timer Notifications
        /* When the Startline/Funline timer is running, a notification is sent after each timebox
        ** is finished saying e.g. "2 minutes Startline timer running until 16:14", with a
        ** "Stop Timer" action button. */
        Intent stopTimerIntent = new Intent(context, MainActivity.class);
        stopTimerIntent.setAction("ACTION_STOP_TIMER");
        PendingIntent stopTimerPendingIntent = PendingIntent.getActivity(
                context,
                STOP_TIMER_REQUEST_CODE,
                stopTimerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent acknowledgeIntent = new Intent(context, MainActivity.class);
        acknowledgeIntent.setAction("ACTION_ACKNOWLEDGE_TIMER_NOTIFICATION");
        PendingIntent acknowledgePendingIntent = PendingIntent.getActivity(
                context,
                7256321,
                acknowledgeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        StringBuilder contextText = new StringBuilder();
        contextText.append(minutesRunning)
                .append(" minutes ")
                .append(isStartline ? "Startline" : "Funline")
                .append(" timer running until ")
                .append(workingUntil)
                .append(". You are ")
                .append(String.format("%.1f", percentCompliant))
                .append("% compliant. Working towards ")
                .append(String.format("%.1f", potentialCompliant))
                .append("% compliance.");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Timer Running")
                .setContentText(contextText.toString())
                .addAction(R.drawable.ic_launcher_foreground, "Stop Timer", stopTimerPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Acknowledge", acknowledgePendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(contextText.toString()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w("NotificationHelper", "Notification permission not granted");
            return; // Don't proceed if permission is not granted
        }

        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        String today = LocalDate.now().toString();
        String shownKey = "shown_" + today;
        int currentTimesShown = prefs.getInt(shownKey, 0);
        prefs.edit().putInt(shownKey, currentTimesShown + 1).apply();
        prefs.edit().putBoolean("timerNotificationAcknowledged", false).apply();

        NotificationManagerCompat.from(context).notify(TIMER_NOTIFICATION_ID, builder.build());
    }

    public static void cancelXModeNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(X_MODE_NOTIFICATION_ID);
        Log.d("NotificationHelper", "X Mode notification cancelled");
    }

    public static void cancelTimerNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(TIMER_NOTIFICATION_ID);
        Log.d("NotificationHelper", "Timer notification cancelled");
    }

    public static void showBreakSuggestionNotification(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w("NotificationHelper", "Notification permission not granted");
            return;
        }

        Intent startBreakIntent = new Intent(context, MainActivity.class);
        startBreakIntent.setAction("ACTION_START_BREAK_TIMER"); // already handled by your MainActivity
        PendingIntent startBreakPendingIntent = PendingIntent.getActivity(
                context,
                START_BREAK_TIMER_REQUEST_CODE,
                startBreakIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Get more steps")
                .setContentText("Put on some music or a podcast — just 2 minutes can reset your mind.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openAppPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Start for 2 Minutes", startBreakPendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(4451, builder.build());
    }
}
