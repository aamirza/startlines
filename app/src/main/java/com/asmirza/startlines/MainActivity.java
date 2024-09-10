package com.asmirza.startlines;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private Handler handler = new Handler();   // will be used to create and cancel timeboxes
    private Runnable timeboxRunnable;  // will be used to create and cancel timeboxes
    private long timeLimitInMillis = Long.MAX_VALUE;  // used when setting a time limit
    private MediaPlayer tickingMediaPlayer;  // will be used for playing the ticking sound
    private List<PendingIntent> alarmPendingIntents = new ArrayList<>();
    private static final int STARTLINE_ALARM_REQUEST_CODE = 0;
    private static final int FUNLINE_ALARM_REQUEST_CODE = 1;
    private static final int MIDNIGHT_ALARM_REQUEST_CODE = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ApplifeCycle", "inside onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Before starting Starline Service");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        Intent serviceIntent = new Intent(this, StartlineService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        loadStatuses();
        addTextChangeListener();
        funModeSwitchListener();
        setupStartButton();
        setupStopButton();
        setupSetTimeLimitButton();
        scheduleStartlines();
        scheduleMidnightAlarm();
        //scheduleStartlineChecker(1, "startline");  // for testing, will schedule Startline in 1 minute
    }

    private void funModeSwitchListener() {
        Switch funModeSwitch = findViewById(R.id.fun_mode_switch);

        funModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("funMode", isChecked);
            editor.apply();
        });
    }

    private void setupStartButton() {
        Button startButton = findViewById(R.id.start_button);
        Log.d("MainActivity", "Setting up start button");

        startButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Start button pressed");
            startTimebox();
        });
    }

    private void setupStopButton() {
        Button stopButton = findViewById(R.id.stop_button);
        Log.d("MainActivity", "Setting up stop button");

        stopButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Stop button pressed");
            stopTimebox();
        });
    }

    private void setupSetTimeLimitButton() {
        Button setTimeLimitButton = findViewById(R.id.time_limit_button);
        Log.d("MainActivity", "Setting up set time limit button");

        setTimeLimitButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Set time limit button pressed");
            showSetTimeLimitDialog();
        });
    }

    @Override
    protected void onPause() {
        Log.d("ApplifeCycle", "App is paused, might be in background");
        super.onPause();
        getAndSaveStatuses();
    }

    @Override
    protected void onResume() {
        Log.d("ApplifeCycle", "App is resumed, back in foreground");
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /* This will execute when the StartlineCheckerReceiver sends an intent to this activity */


        super.onNewIntent(intent);
        setIntent(intent); // Important: update the activity's intent
        Log.d("MainActivity", "onNewIntent called with intent: " + intent.getStringExtra("lineType"));

        String lineType = intent.getStringExtra("lineType");

        if (lineType != null) {
            if (lineType.equals("midnight")) {
                scheduleStartlines();
            } else {
                Log.d("onNewIntent", "executing lineType: " + lineType);
                executeStartline(lineType);
            }
        }
    }

    public void saveStatuses(String startlineStatus, String funlineStatus, String taskName, boolean funMode) {
        /* Save Startlines, Funline, and Task Name in case of reboot or app close */
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("startlineStatus", startlineStatus);
        editor.putString("funlineStatus", funlineStatus);
        editor.putString("taskName", taskName);
        editor.putBoolean("funMode", funMode);

        editor.apply(); // Saves changes asynchronously
    }

    public void loadStatuses() {
        /* Load Startlines, Funline, and Task Name after reboot or app close */
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);

        try {
            String startlineStatus = sharedPreferences.getString("startlineStatus", "0");
            String funlineStatus = sharedPreferences.getString("funlineStatus", "0");
            String taskName = sharedPreferences.getString("taskName", "");
            boolean funMode = sharedPreferences.getBoolean("funMode", false);

            TextView startlineStatusTextView = findViewById(R.id.startline_status);
            TextView funlineStatusTextView = findViewById(R.id.funline_status);
            TextView taskNameTextView = findViewById(R.id.task_name);
            Switch funModeSwitch = findViewById(R.id.fun_mode_switch);


            startlineStatusTextView.setText(startlineStatus);
            funlineStatusTextView.setText(funlineStatus);
            taskNameTextView.setText(taskName);
            funModeSwitch.setChecked(funMode);
        } catch (Exception e) {
            saveStatuses("0", "0", "", false);
            loadStatuses();
        }
    }

    public void getAndSaveStatuses() {
        /* Get and Save Startlines, Funline, and Task Name */
        TextView startlineStatusTextView = findViewById(R.id.startline_status);
        TextView funlineStatusTextView = findViewById(R.id.funline_status);
        EditText taskNameTextView = findViewById(R.id.task_name);
        Switch funModeSwitch = findViewById(R.id.fun_mode_switch);

        String startlineStatus = startlineStatusTextView.getText().toString();
        String funlineStatus = funlineStatusTextView.getText().toString();
        String taskName = taskNameTextView.getText().toString();
        boolean funMode = funModeSwitch.isChecked();

        saveStatuses(startlineStatus, funlineStatus, taskName, funMode);
    }

    public boolean isFunModeOn() {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return prefs.getBoolean("funMode", false);
    }

    public boolean isTimeLimitPassed() {
        return System.currentTimeMillis() >= timeLimitInMillis;
    }


    public void addTextChangeListener() {
        EditText taskNameEditText = findViewById(R.id.task_name);
        taskNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getAndSaveStatuses();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public void scheduleStartlines() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

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
                    Log.d("Scheduler", "Alarm time is in the past, skipping: " + calendar.getTime().toString());
                    continue;
                }

                String lineType;
                if (minute % funlineInterval == 0) {  // if it is divisible by 30 (funlineInterval)
                    lineType = "funline";
                } else {
                    lineType = "startline";
                }

                Intent intent = new Intent(this, StartlineCheckerReceiver.class);
                int requestCode = Integer.parseInt(String.valueOf(i*60 + minute));
                intent.putExtra("lineType", lineType);
                intent.putExtra("requestCode", requestCode);
                // create a time using i and minute then convert to int
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                alarmPendingIntents.add(pendingIntent);
                Log.d("Scheduler", "Next alarm set for " + lineType + " at " + calendar.getTime().toString());
            }
        }

        Log.d("Scheduler", "All alarms for the day set");
    }

    private void scheduleMidnightAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, StartlineCheckerReceiver.class);
        intent.putExtra("lineType", "midnight");
        intent.putExtra("requestCode", MIDNIGHT_ALARM_REQUEST_CODE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, MIDNIGHT_ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, 1);

        long alarmTimeInMillis = calendar.getTimeInMillis();

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarmTimeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent);
        Log.d("Scheduler", "Midnight alarm set for " + calendar.getTime().toString() + " and set to repeat after that");
    }

    public void startTimebox() {
        startTimebox(2);
    }

    public void startTimebox(int currentTimeboxDuration) {
        /* For starting the 2 minute staircase timeboxes when the start button is pressed */

        int timeboxDurationInMillis = currentTimeboxDuration * 60 * 1000;
        long endTimeInMillis = timestampMinutesFromNow(currentTimeboxDuration);


        String endTimeFormatted = timestampToText(endTimeInMillis);

        if (timeLimitInMillis == Long.MAX_VALUE) {
            // If there is no time limit set, play the ticking sound
            playTickingSound();
        }

        setTimeboxStatusText(currentTimeboxDuration + " (" + endTimeFormatted + ")");
        Log.d("Timebox", "Timebox started for " + currentTimeboxDuration + " minutes, ending at " + endTimeFormatted);
        // Set Startlines to "1" after timebox completion and start the new timebox

        timeboxRunnable = () -> {
            timeboxComplete();
            if (isTimeLimitPassed()) {
                Log.d("Timebox", "Time limit reached, stopping timebox");
                stopTimebox();
                return;
            }
            int nextTimeboxDuration = currentTimeboxDuration + 2;
            startTimebox(nextTimeboxDuration);
        };

        handler.postDelayed(timeboxRunnable, timeboxDurationInMillis);
    }

    public void stopTimebox() {
        /* For stopping the timebox when the stop button is pressed */
        if (timeboxRunnable != null) {
            handler.removeCallbacks(timeboxRunnable);
            setTimeboxStatusText("0");
            resetWorkingUntilTime();
            stopTickingSound();

            Log.d("Timebox", "Timebox stopped");
        }
    }

    private void showSetTimeLimitDialog() {
        /* Allow users to set a time limit for their timebox using a dialog box */
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Time Limit");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String inputText = input.getText().toString();
            if (!inputText.isEmpty()) {
                int minutes = Integer.parseInt(inputText);
                timeLimitInMillis = timestampMinutesFromNow(minutes);
                String endTimeText = timestampToText(timeLimitInMillis);
                setWorkingUntilText(endTimeText);
                stopTickingSound();
                Log.d("Timebox", "Time limit set for " + minutes + " minutes");
            } else {
                resetWorkingUntilTime();
                Log.d("Timebox", "Time limit cleared");
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void setStartlineStatus(String i) {
        TextView startlineStatusTextView = findViewById(R.id.startline_status);
        startlineStatusTextView.setText(i);
        getAndSaveStatuses();
    }

    private void setFunlineStatus(String i) {
        TextView funlineStatusTextView = findViewById(R.id.funline_status);
        funlineStatusTextView.setText(i);
        getAndSaveStatuses();
    }

    private void setLineStatus(String lineType, String status) {
        if (lineType.equals("startline")) {
            setStartlineStatus(status);
        } else if (lineType.equals("funline")) {
            setFunlineStatus(status);
        }
    }

    private void setTimeboxStatusText(String text) {
        TextView currentTimeboxTextView = findViewById(R.id.current_timebox);
        currentTimeboxTextView.setText(text);
    }

    private void setWorkingUntilText(String timeText) {
        TextView workingUntilTextView = findViewById(R.id.working_until_text);
        Log.d("MainActivity", "Setting working until text to: " + timeText);
        if (!timeText.isEmpty()) {
            String formattedText = getString(R.string.working_until_text, timeText);
            workingUntilTextView.setText(formattedText);
            Log.d("MainActivity", "Working until text set to: " + formattedText);
        } else {
            workingUntilTextView.setText("");
            Log.d("MainActivity", "Working until text cleared");
        }
    }

    private void resetWorkingUntilTime() {
        setWorkingUntilText("");
        timeLimitInMillis = Long.MAX_VALUE;
    }

    private void timeboxComplete() {
        /* Called when the timebox is complete */
        if (isFunModeOn()) {
            setFunlineStatus("1");
            Log.d("MainActivity", "Funline set to 1 after timebox completion");
        } else {
            setStartlineStatus("1");
            Log.d("MainActivity", "Startline set to 1 after timebox completion");
        }
    }

    public void executeStartline(String lineType) {
        /* Checks Startline status and sets it accordingly */
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        String status = prefs.getString(lineType + "Status", "0");

        if (status.equals("0") || status.equals("X")) {
            setLineStatus(lineType, "X");
            scheduleStartlineChecker(5, lineType);
        } else if (status.equals("1")) {
            setLineStatus(lineType, "0");
        }
    }

    public void scheduleStartlineChecker(int intervalInMinutes, String lineType) {
        /* Schedules the Startline Manager to run after a certain amount of minutes */
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, StartlineCheckerReceiver.class);
        int requestCode = lineType.equals("startline") ? STARTLINE_ALARM_REQUEST_CODE : FUNLINE_ALARM_REQUEST_CODE;
        intent.putExtra("lineType", lineType);
        intent.putExtra("requestCode", requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long interval = (long) intervalInMinutes * 60 * 1000;  // in milliseconds
        long triggerAtMillis = System.currentTimeMillis() + interval;

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        Log.d("Scheduler", "Startline Checker scheduled for " + lineType + " in " + intervalInMinutes + " minutes");
    }

    private void playTickingSound() {
        if (tickingMediaPlayer == null) {
            tickingMediaPlayer = MediaPlayer.create(this, R.raw.clock_tick);
            tickingMediaPlayer.setLooping(true);
        }
        tickingMediaPlayer.start();
    }

    private void stopTickingSound() {
        if (tickingMediaPlayer != null) {
            tickingMediaPlayer.stop();
            tickingMediaPlayer.reset();  // Reset the media player
            tickingMediaPlayer.release();
            tickingMediaPlayer = null;
        }
    }

    private static String timestampToText(long timestamp) {
        /* Converts a timestamp to a human-readable HH:mm format */
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(timestamp);
    }

    private static long timestampMinutesFromNow(int minutes) {
        /* Returns a timestamp for a certain amount of minutes from the current time */
        return System.currentTimeMillis() + ((long) minutes * 60 * 1000);
    }
}