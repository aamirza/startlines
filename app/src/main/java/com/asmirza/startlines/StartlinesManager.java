/* Startlines code that is common to both the StartlineService and MainActivity */

package com.asmirza.startlines;


import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StartlinesManager {
    private static final int STARTLINE_ALARM_REQUEST_CODE = 0;
    private static final int FUNLINE_ALARM_REQUEST_CODE = 1;
    private static final int MIDNIGHT_ALARM_REQUEST_CODE = 2000;

    private List<PendingIntent> alarmPendingIntents = new ArrayList<>();

    /*********************** Setting some universal variables ************************/

    public static void setStartlineStatus(Context context, String i) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Log.d("StartlineManager", "Setting startline status to " + i);

        editor.putString("startlineStatus", i);
        editor.apply();
    }

    public static void setFunlineStatus(Context context, String i) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Log.d("StartlineManager", "Setting funline status to " + i);

        editor.putString("funlineStatus", i);
        editor.apply();
    }

    public static void setLineStatus(Context context, String lineType, String status) {
        if (lineType.equals("startline")) {
            setStartlineStatus(context, status);
        } else if (lineType.equals("funline")) {
            setFunlineStatus(context, status);
        }
    }

    public static void incrementStartlinesMissed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int startlinesMissed = prefs.getInt("startlinesMissed", 0);
        editor.putInt("startlinesMissed", startlinesMissed + 1);
        Log.d("StartlineManager", "Startlines missed: " + (startlinesMissed + 1));
        editor.apply();
    }

    public static void resetStartlinesMissed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt("startlinesMissed", 0);
        Log.d("StartlineManager", "Startlines missed reset");
        editor.apply();
    }

    /*********************** Querying the state of startlines ************************/

    public static boolean isTimeboxRunning(Context context) {
        Log.d("StartlinesManager", "Checking if timebox is running");
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        boolean timeboxRunning = prefs.getBoolean("workingStatus", false);
        Log.d("StartlinesManager", "Timebox running: " + timeboxRunning);
        return timeboxRunning;
    }

    /*********************** Code for executing or scheduling startlines ************************/

    public static void scheduleStartlines(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        int[] startlineMinutes = {0, 14, 29, 30, 44, 59};
        int funlineInterval = 30;  // Funline is every 30 minutes

        int hoursInADay = 24;
        for (int i = 0; i < hoursInADay; i++) {
            for (int minute : startlineMinutes) {
                calendar.set(Calendar.HOUR_OF_DAY, i);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                long alarmTimeInMillis = calendar.getTimeInMillis();

                if (alarmTimeInMillis < System.currentTimeMillis()) {
                    Log.d("StartlineManager Scheduler", "Alarm time is in the past, skipping: " + calendar.getTime().toString());
                    continue;
                }

                String lineType;
                if (minute % funlineInterval == 0) {  // if it is divisible by 30 (funlineInterval)
                    lineType = "funline";
                } else {
                    lineType = "startline";
                }

                Intent intent = new Intent(context, StartlineCheckerReceiver.class);
                int requestCode = Integer.parseInt(String.valueOf(i*60 + minute));
                intent.putExtra("lineType", lineType);
                intent.putExtra("requestCode", requestCode);
                // create a time using i and minute then convert to int
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                Log.d("StartlineManager Scheduler", "Next alarm set for " + lineType + " at " + calendar.getTime().toString());
            }
        }

        Log.d("Scheduler", "All alarms for the day set");
    }

    public static void executeStartlines(Context context, String lineType) {
        Log.d("StartlineManager", "Executing startlines for " + lineType);
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String status = prefs.getString(lineType + "Status", "0");

        if (status.equals("0") || status.equals("X")) {
            setLineStatus(context, lineType, "X");
            incrementStartlinesMissed(context);
            scheduleStartlineChecker(context,5, lineType);
        } else if (status.equals("1")) {
            setLineStatus(context, lineType, "0");
            resetStartlinesMissed(context);
        }
    }

    public static void scheduleStartlineChecker(Context context, int minutes, String lineType) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(context, StartlineCheckerReceiver.class);
        int requestCode = lineType.equals("startline") ? STARTLINE_ALARM_REQUEST_CODE : FUNLINE_ALARM_REQUEST_CODE;
        intent.putExtra("lineType", lineType);
        intent.putExtra("requestCode", requestCode);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long interval = (long) minutes * 60 * 1000;
        long triggerAtMillis = System.currentTimeMillis() + interval;
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);

        Log.d("StartlineManager Scheduler", "Startline checker scheduled for " + minutes + " minutes");
    }

    /*********************** Startlines code for app blocking ************************/

    public static boolean isAppInBlockingMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        String startlineStatus = prefs.getString("startlineStatus", "0");
        String funlineStatus = prefs.getString("funlineStatus", "0");
        int startLinesMissed = prefs.getInt("startlinesMissed", 0);

        if (!startlineStatus.equals("X") && !funlineStatus.equals("X")) {
            return false;
        }

        boolean bothAreX = startlineStatus.equals("X") && funlineStatus.equals("X");
        boolean oneIsXAndOneIs0 = (startlineStatus.equals("X") && funlineStatus.equals("0")) || (startlineStatus.equals("0") && funlineStatus.equals("X"));
        boolean oneIsXAndOneIs1 = (startlineStatus.equals("X") && funlineStatus.equals("1")) || (startlineStatus.equals("1") && funlineStatus.equals("X"));
        Log.d("AppBlockingAccessiblityService", "Startlines missed: " + startLinesMissed);

        return bothAreX || (oneIsXAndOneIs0 && startLinesMissed >= 1) || (oneIsXAndOneIs1 && startLinesMissed >= 2);
    }

    public static Set<String> getBlockedApps(Context context) {
        Log.d("StartlinesManager", "Getting blocked apps");
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getStringSet("blockedApps", new HashSet<>());
    }

    public static boolean isAppBlocked(Context context, String packageName) {
        Log.d("StartlinesManager", "Checking if app is blocked: " + packageName);
        Set<String> blockedApps = getBlockedApps(context);
        boolean isAppBlocked = blockedApps.contains(packageName);
        Log.d("StartlinesManager", "App is blocked: " + isAppBlocked);
        return isAppBlocked;
    }

    public static void blockApp(Context context, String packageName) {
        if (isAppInBlockingMode(context)) {
            Log.d("StartlinesManager", "Conditions for blocking app met: " + packageName);
            openStartlinesApp(context);
        }
    }

    public static void openStartlinesApp(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!isTimeboxRunning(context)) {
            context.startActivity(intent);
        }
    }

}
