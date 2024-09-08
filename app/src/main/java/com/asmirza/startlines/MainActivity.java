package com.asmirza.startlines;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ApplifeCycle", "inside onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Before starting Starline Service");
        // resetting if there's an error
        //saveStatuses("0", "0", "", false);
        Intent serviceIntent = new Intent(this, StartlineService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        loadStatuses();
        addTextChangeListener();
        funModeSwitchListener();
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

    public void startTimebox() {
        /* For starting the 2 minute staircase timeboxes when the start button is pressed */
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int twoMinutes = 2 * 60 * 1000;  // in milliseconds
        long currentTime = System.currentTimeMillis();
        int currentTimeboxDuration = prefs.getInt("timeboxDuration", twoMinutes);

        long endTime = currentTime + currentTimeboxDuration;

        editor.putLong("endTime", endTime);
        editor.apply();

        // Set Startlines to "1" after timebox completion and start the new timebox
        new Handler().postDelayed(() -> {
            if (isFunModeOn()) {
                setFunlineStatus("1");
            } else {
                setStartlineStatus("1");
            }
            int nextTimeboxDuration = currentTimeboxDuration + twoMinutes;
            editor.putInt("timeboxDuration", nextTimeboxDuration);
            editor.apply();
            startTimebox(); // Start the next timebox automatically
        }, currentTimeboxDuration);
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
        /* Schedules the Startline Manager to run every 5 seconds */
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, StartlineCheckerReceiver.class);
        intent.putExtra("lineType", lineType);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        long interval = (long) intervalInMinutes * 60 * 1000;  // in milliseconds
        long triggerAtMillis = System.currentTimeMillis() + interval;

        // TODO: You may need to make this setExactAndAllowWhileIdle if this is not reliable
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }
}