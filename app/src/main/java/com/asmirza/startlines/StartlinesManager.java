/* Startlines code that is common to both the StartlineService and MainActivity */

package com.asmirza.startlines;


import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public static void setFunMode(Context context, boolean funMode) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("funMode", funMode);
        Log.d("StartlineManager", "Fun mode set: " + funMode);
        editor.apply();
    }

    public static void setMusicMode(Context context, boolean musicMode) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("musicMode", musicMode);
        Log.d("StartlineManager", "Music mode set: " + musicMode);
        editor.apply();
    }

    public static void setMusicModeOn(Context context) {
        setMusicMode(context, true);
    }

    public static void setTickingSoundPlaying(Context context, boolean tickingSoundPlaying) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("tickingSoundPlaying", tickingSoundPlaying);
        Log.d("StartlineManager", "Ticking sound playing: " + tickingSoundPlaying);
        editor.apply();
    }

    /*********************** Querying the state of startlines ************************/

    public static boolean isTimeboxRunning(Context context) {
        Log.v("StartlinesManager", "Checking if timebox is running");
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        boolean timeboxRunning = prefs.getBoolean("workingStatus", false);
        Log.v("StartlinesManager", "Timebox running: " + timeboxRunning);
        return timeboxRunning;
    }

    public static boolean isScreenOn(Context context) {
        Log.d("StartlinesManager", "Checking if screen is off");
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean screenOn = powerManager.isInteractive();
        Log.d("StartlinesManager", "Screen on: " + screenOn);
        return screenOn;
    }

    public static boolean isMusicModeOn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        boolean musicMode = prefs.getBoolean("musicMode", false);
        Log.d("StartlinesManager", "Music mode: " + musicMode);
        return musicMode;
    }

    public static boolean isTickingSoundPlaying(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        boolean tickingSoundPlaying = prefs.getBoolean("tickingSoundPlaying", false);
        Log.d("StartlinesManager", "Ticking sound playing: " + tickingSoundPlaying);
        return tickingSoundPlaying;
    }

    public static String getStartlineStatus(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return prefs.getString("startlineStatus", "0");
    }

    public static String getFunlineStatus(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return prefs.getString("funlineStatus", "0");
    }

    public static String getIpAddress(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return prefs.getString("serverIp", "192.168.2.16");
    }

    public static int getPort(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return prefs.getInt("serverPort", 12345);
    }

    /*********************** Code for vibrating the phone during block ************************/

    public static void startVibrationLoop(Context context) {
        Log.d("StartlinesManager", "Starting vibration loop");
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        new Thread(() -> {
            while (isAppBlockingModeOn(context) && !isTimeboxRunning(context)
                    && audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                if (!isScreenOn(context)) {
                    Log.d("StartlinesManager", "Screen is off, vibrating");

                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        VibrationEffect effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);
                        vibrator.vibrate(effect);
                    }
                } else {
                    Log.d("StartlinesManager", "Screen is on, not vibrating");
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            Log.d("StartlinesManager", "Vibration loop stopped. Timebox started, phone is silent, or blocking conditions not met.");
        }).start();
    }

    /*********************** Code for pausing music ************************/

    public static void pauseMusic(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (audioManager.isMusicActive()) {
            Log.d("StartlinesManager", "Pausing music");

            KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE);
            audioManager.dispatchMediaKeyEvent(keyEvent);

            keyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE);
            audioManager.dispatchMediaKeyEvent(keyEvent);

            Log.d("StartlinesManager", "Music paused");
        } else {
            Log.d("StartlinesManager", "No music playing");
        }
    }

    private static void resumeMusic(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (!audioManager.isMusicActive()) {
            Log.d("StartlinesManager", "Resuming music");

            KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY);
            audioManager.dispatchMediaKeyEvent(keyEvent);

            keyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY);
            audioManager.dispatchMediaKeyEvent(keyEvent);

            Log.d("StartlinesManager", "Music resumed");
        } else {
            Log.d("StartlinesManager", "Music is already playing");
        }
    }

    public static void startMusicPauseLoop(Context context) {
        Log.d("StartlinesManager", "Starting music pause loop");
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AtomicBoolean musicWasPlaying = new AtomicBoolean(audioManager.isMusicActive());
        new Thread(() -> {
            while (isAppBlockingModeOn(context) && !isTimeboxRunning(context)
                    && audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {

                if (!isScreenOn(context)) {
                    boolean musicIsActiveNow = audioManager.isMusicActive();

                    if (!musicWasPlaying.get() && musicIsActiveNow) {
                        Log.d("StartlinesManager", "Music was not playing when X'd initially, but is playing now.");
                        musicWasPlaying.set(true);
                    }

                    if (musicWasPlaying.get()) {
                        if (musicIsActiveNow) {
                            Log.d("StartlinesManager", "Music is playing and screen is off while X'd, pausing music.");
                            pauseMusic(context);
                        } else {
                            Log.d("StartlinesManager", "Resuming music as part of X mode music loop.");
                            resumeMusic(context);
                        }
                    }
                }

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            Log.d("StartlinesManager", "Music pause loop stopped. Timebox started, phone is silent, or blocking conditions not met.");
        }).start();
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
            sendStartlineMessageToServer(context);
            if (isAppBlockingModeOn(context) && !isTimeboxRunning(context)) {
                openCalendarApp(context);
                startVibrationLoop(context);
                startMusicPauseLoop(context);
                NotificationHelper.showXModeNotification(context, getStartlineStatus(context).equals("X"));
            }
            scheduleStartlineChecker(context,5, lineType);
        } else if (status.equals("1")) {
            setLineStatus(context, lineType, "0");
            sendStartlineMessageToServer(context);
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

    public static boolean isAppBlockingModeOn(Context context) {
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

    public static boolean isAppBlockingModeOnOrBelowMinimumCompliance(Context context) {
        return isBelowMinimumComplianceScore(context) || isAppBlockingModeOn(context);
    }

    public static Set<String> getBlockedApps(Context context) {
        Log.v("StartlinesManager", "Getting blocked apps");
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getStringSet("blockedApps", new HashSet<>());
    }

    public static Set<String> getDistractingApps(Context context) {
        Log.d("StartlinesManager", "Getting distracting apps");
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getStringSet("distractingApps", new HashSet<>());
    }

    public static boolean isAppBlocked(Context context, String packageName) {
        Log.v("StartlinesManager", "Checking if app is blocked: " + packageName);
        Set<String> blockedApps = getBlockedApps(context);
        boolean isAppBlocked = blockedApps.contains(packageName);
        Log.v("StartlinesManager", "App is blocked: " + isAppBlocked);
        return isAppBlocked;
    }

    public static boolean isAppDistracting(Context context, String packageName) {
        Log.d("StartlinesManager", "Checking if app is distracting: " + packageName);
        Set<String> distractingApps = getDistractingApps(context);
        boolean isAppDistracting = distractingApps.contains(packageName);
        Log.d("StartlinesManager", "App is blocked when working: " + isAppDistracting);
        return isAppDistracting;
    }

    public static void blockApp(Context context, String packageName) {
        Log.d("StartlinesManager", "Conditions for blocking app met: " + packageName);
        openCalendarApp(context);
        NotificationHelper.showXModeNotification(context, getStartlineStatus(context).equals("X"));
    }

    public static void blockDistractingApp(Context context, String packageName) {
        Log.d("StartlinesManager", "Conditions for blocking distracting app met: " + packageName);
        openCalendarApp(context);
    }

    public static void openStartlinesApp(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void openCalendarApp(Context context) {
        Set<String> calendarApp = getCalendarApp(context);
        if (calendarApp.isEmpty()) {
            Log.d("StartlinesManager", "No calendar app selected");
            return;
        }

        List<String> calendarAppList = new ArrayList<>(calendarApp);
        int randomIndex = (int) (Math.random() * calendarAppList.size());
        String randomCalendarApp = calendarAppList.get(randomIndex);

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(randomCalendarApp);
        if (launchIntent != null) {
            Log.d("StartlinesManager", "Opening calendar app: " + randomCalendarApp);
            context.startActivity(launchIntent);
        }
    }

    public static void blockAppIfNecessary(Context context, String packageName) {
        boolean working = StartlinesManager.isTimeboxRunning(context);

        if (working && isAppDistracting(context, packageName)) {
            Log.d("StartlinesManager Blocker", "Distracting app detected, blocking: " + packageName);
            // For blocking distracting apps when working
            if (isMusicModeOnAndAppNotPlayingMedia(context)) {
                openRandomMusicApp(context);
            } else {
                blockDistractingApp(context, packageName);
            }
        } else if (!working && isAppBlocked(context, packageName)) {
            Log.d("AppBlockingAccessiblityService", "Blocked app detected: " + packageName);
            if (isAppBlockingModeOnOrBelowMinimumCompliance(context)) {
                blockApp(context, packageName);
            } else if (isMusicModeOnAndAppNotPlayingMedia(context)) {
                Log.d("StartlinesManager Blocker", "Music mode on and no music app playing media");
                openRandomMusicApp(context);
            }
        }
    }

    /*********************** Music-mode related code ************************/

    public static Set<String> getMusicApps(Context context) {
        Log.d("StartlinesManager", "Getting music apps");
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        Set<String> musicApps = sharedPreferences.getStringSet("musicApps", new HashSet<>());
        return musicApps;
    }

    public static Set<String> getCalendarApp(Context context) {
        Log.d("StartlinesManager", "Getting calendar apps");
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        Set<String> calendarApp = sharedPreferences.getStringSet("calendarApp", new HashSet<>());
        return calendarApp;
    }

    public static void openRandomMusicApp(Context context) {
        Set<String> musicApps = getMusicApps(context);
        if (musicApps.isEmpty()) {
            Log.d("StartlinesManager", "No music apps selected");
            return;
        }

        List<String> musicAppsList = new ArrayList<>(musicApps);
        int randomIndex = (int) (Math.random() * musicAppsList.size());
        String randomMusicApp = musicAppsList.get(randomIndex);

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(randomMusicApp);
        if (launchIntent != null) {
            Log.d("StartlinesManager", "Opening music app: " + randomMusicApp);
            context.startActivity(launchIntent);
        }
    }

    public static boolean isMusicModeAppPlayingMedia(Context context) {
        if (isTickingSoundPlaying(context)) {
            Log.d("StartlinesManager", "Ticking sound is playing, assuming no music is playing.");
            return false;
        }

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        boolean musicPlaying = audioManager.isMusicActive();

        Log.d("StartlinesManager", "Music playing: " + musicPlaying);
        return musicPlaying;
    }

    public static boolean isMusicModeOnAndAppNotPlayingMedia(Context context) {
        boolean musicMode = isMusicModeOn(context);
        boolean appPlayingMedia = isMusicModeAppPlayingMedia(context);

        return musicMode && !appPlayingMedia;
    }

    private static String createStatusMessage(Context context) {
        String startlineStatus = getStartlineStatus(context);
        String funlineStatus = getFunlineStatus(context);
        String workingStatus = isTimeboxRunning(context) ? "1" : "0";
        String appBlockingMode = isAppBlockingModeOn(context) ? "1" : "0";
        return "{ \"startline\": \"" + startlineStatus + "\", " +
                "\"funline\": \"" + funlineStatus + "\", " +
                "\"working\": \"" + workingStatus + "\", " +
                "\"blocked\": \"" + appBlockingMode + "\" }";
    }

    public static void sendStartlineMessageToServer(Context context) {
        Log.d("MainActivity", "Sending status message to server");
        SocketClient.sendMessageToServer(createStatusMessage(context), getIpAddress(context), getPort(context));
    }

    /*********************** Timebox related code ************************/

    public static List<Timebox> loadTimeboxes(Context context, boolean includeIncomplete) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("timeboxes", "[]");
        Type type = new TypeToken<ArrayList<Timebox>>() {}.getType();
        Log.d("StartlinesManager", "Timeboxes loaded from SharedPreferences in StartlinesManager");
        List<Timebox> timeboxes = gson.fromJson(json, type);

        if (!includeIncomplete) {
            List<Timebox> completeTimeboxes = new ArrayList<>();
            for (Timebox timebox : timeboxes) {
                if (timebox.isComplete()) {
                    completeTimeboxes.add(timebox);
                }
            }
            return completeTimeboxes;
        } else {
            return timeboxes;
        }
    }

    public static void saveTimeboxes(Context context, List<Timebox> timeboxList) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String json = gson.toJson(timeboxList);
        editor.putString("timeboxes", json);
        editor.apply();

        Log.d("Timebox", "Timeboxes saved to SharedPreferences");
    }

    /*********************** Code related to schedule compliance ************************/
    public static int getMinimumComplianceScore(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("minimumComplianceScore", 20);
    }

    public static void setMinimumComplianceScore(Context context, int score) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("minimumComplianceScore", score);
        editor.apply();
    }

    public static void saveComplianceScore(Context context, int score) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("complianceScore", score);
        editor.apply();
    }

    public static int getComplianceScore(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("complianceScore", 0);
    }

    public static boolean isBelowMinimumComplianceScore(Context context) {
        int complianceScore = getComplianceScore(context);
        int minimumComplianceScore = getMinimumComplianceScore(context);
        return complianceScore < minimumComplianceScore;
    }
}
