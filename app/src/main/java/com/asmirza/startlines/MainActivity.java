package com.asmirza.startlines;

import static com.asmirza.startlines.NotificationHelper.NOTIFICATION_PERMISSION_REQUEST_CODE;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private boolean workingStatus = false;  // 0 if timebox is not running, 1 if timebox is running
    private Handler handler = new Handler();   // will be used to create and cancel timeboxes
    private Runnable timeboxRunnable;  // will be used to create and cancel timeboxes
    private Runnable startlineSnoozer; // will be used to snooze startlines
    private long timeLimitInMillis = Long.MAX_VALUE;  // used when setting a time limit
    private Vibrator vibrator;
    private MediaPlayer tickingMediaPlayer;  // will be used for playing the ticking sound
    private int startlinesMissed = 0;  // how many times you missed a startline, will be used to determine whether to block apps or not
    private List<PendingIntent> alarmPendingIntents = new ArrayList<>();
    private TaskAdapter taskAdapter;
    private List<Task> taskList = new ArrayList<>();
    private TextInputEditText taskInput;
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
        musicModeSwitchListener();
        setupStartButton();
        setupStopButton();
        setupSetTimeLimitButton();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityServiceDialog();
        }
        checkNotificationPermission();
        NotificationHelper.createNotificationChannel(this);
        scheduleStartlines();
        scheduleMidnightAlarm();
        setupBackPressHandler();
        setupTaskList();
        StartlinesManager.sendStartlineMessageToServer(this);
        NotificationHelper.showPermanentNotification(this, getStartlineStatus(), getFunlineStatus());
        scheduleStartlineChecker(1, "startline");  // for testing, will schedule Startline in 1 minute
    }

    private void setupTaskList() {
        taskInput = findViewById(R.id.task_name);
        RecyclerView taskRecyclerView = findViewById(R.id.todo_recycler_view);
        taskAdapter = new TaskAdapter(taskList);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskRecyclerView.setAdapter(taskAdapter);

        TextInputLayout taskInputLayout = findViewById(R.id.task_add_icon);
        taskInputLayout.setEndIconOnClickListener(v -> {
            addTask();
        });
    }

    private void addTask() {
        TextInputLayout taskInputLayout = findViewById(R.id.task_add_icon);
        String taskName = taskInput.getText().toString().trim();

        if (!taskName.isEmpty()) {
            taskList.add(new Task(taskName));
            taskAdapter.notifyItemInserted(taskList.size() - 1);
            taskInput.setText("");
        } else {
            taskInputLayout.setError("Task name cannot be empty");
            taskInputLayout.postDelayed(() -> taskInputLayout.setError(null), 2000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.manage_blocked_apps) {
            Log.d("MainActivity", "Manage blocked apps button pressed");
            Intent intent = new Intent(this, AppBlockingActivity.class);
            intent.putExtra("blockType", "X_MODE_BLOCK");
            startActivity(intent);
            return true;
        } else if (id == R.id.manage_distracting_apps) {
            // Open the Manage Distracting Apps screen
            Log.d("MainActivity", "Manage distracting apps button pressed");
            Intent intent = new Intent(this, AppBlockingActivity.class);
            intent.putExtra("blockType", "DISTRACTING_APPS_BLOCK");
            startActivity(intent);
            return true;
        } else if (id == R.id.manage_music_apps) {
            // Open the Manage Music Apps screen
            Log.d("MainActivity", "Manage music apps button pressed");
            Intent intent = new Intent(this, AppBlockingActivity.class);
            intent.putExtra("blockType", "MUSIC_APPS");
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.ip_port_settings) {
            openIPSettingsDialog();
            return true;
        } else if (item.getItemId() == R.id.snooze_startlines) {
            snooze();
            Log.d("MainActivity", "Snooze Startlines button pressed");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                // Permission already granted
                Log.d("Notification", "Notification permission already granted");
                NotificationHelper.showPermanentNotification(this, getStartlineStatus(), getFunlineStatus());
            }
        } else {
            // No need to check for permission
            Log.d("Notification", "Notification permission not needed");
            NotificationHelper.showPermanentNotification(this, getStartlineStatus(), getFunlineStatus());
        }
    }

    private void updatePermanentNotification() {
        NotificationHelper.showPermanentNotification(this, getStartlineStatus(), getFunlineStatus());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Log.d("Notification", "Notification permission granted");
            } else {
                // Permission denied
                Log.d("Notification", "Notification permission denied");
            }
        }
    }


    private void funModeSwitchListener() {
        Switch funModeSwitch = findViewById(R.id.fun_mode_switch);

        funModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            StartlinesManager.setFunMode(this, isChecked);
        });
    }

    private void musicModeSwitchListener() {
        Switch musicModeSwitch = findViewById(R.id.music_mode_switch);

        musicModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            StartlinesManager.setMusicMode(this, isChecked);
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
        loadStatuses();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.d("ApplifeCycle", "App is destroyed");
        if (tickingMediaPlayer != null) {
            stopTickingSound();
        }
        killTimeboxHandler();
        getAndSaveStatuses();
        super.onDestroy();
    }

    private void setupBackPressHandler() {
        /* This will handle the back button press */
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d("MainActivity", "Back button pressed");
                moveTaskToBack(true);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /* This will execute when the StartlineCheckerReceiver sends an intent to this activity */
        // Also used for notification action buttons

        Log.d("MainActivity", "onNewIntent called");

        super.onNewIntent(intent);
        setIntent(intent); // Important: update the activity's intent
        String action = intent.getAction();

        if (action != null) {
            switch (action) {
                case "ACTION_START_STARTLINE_TIMER":
                    Log.d("MainActivity", "Start Startline Timer notification button pressed");
                    if (!isWorking()) {
                        switchFunModeToOn(false);
                        startTimebox();
                    } else {
                        Toast.makeText(this, "Timebox is already running.", Toast.LENGTH_SHORT).show();
                        Log.w("MainActivity", "Timebox already running, not starting a new one");
                    }
                    break;
                case "ACTION_START_FUNLINE_TIMER":
                    Log.d("MainActivity", "Start Funline Timer notification button pressed");
                    if (!isWorking()) {
                        switchFunModeToOn(true);
                        startTimebox();
                    } else {
                        Toast.makeText(this, "Timebox is already running.", Toast.LENGTH_SHORT).show();
                        Log.w("MainActivity", "Timebox already running, not starting a new one");
                    }
                    break;
                case "ACTION_START_TIMER":
                    Log.d("MainActivity", "Start Timer notification button pressed");
                    if (!isWorking()) {
                        if (getStartlineStatus() == "X") {
                            switchFunModeToOn(false);
                            startTimebox();
                        } else if (getFunlineStatus() == "X") {
                            switchFunModeToOn(true);
                            startTimebox();
                        } else {
                            Toast.makeText(this, "Neither Startline or Funline are X'd. Timer not started.", Toast.LENGTH_SHORT).show();
                            Log.w("MainActivity", "Neither Startline or Funline are X'd. Timer not started.");
                        }
                    } else {
                        Toast.makeText(this, "Timebox is already running.", Toast.LENGTH_SHORT).show();
                        Log.w("MainActivity", "Timebox already running, not starting a new one");
                    }
                    break;
                case "ACTION_STOP_TIMER":
                    Log.d("MainActivity", "Stop Timer notification button pressed");
                    stopTimebox();
                    break;
                case "ACTION_SNOOZE":
                    Log.d("MainActivity", "Snooze button pressed");
                    if (!isWorking()) {
                        snooze();
                    }
                    break;
            }
        } else {
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
    }

    public void saveStatuses(String startlineStatus, String funlineStatus, String taskName, boolean funMode, boolean musicMode) {
        /* Save Startlines, Funline, and Task Name in case of reboot or app close */
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("startlineStatus", startlineStatus);
        editor.putString("funlineStatus", funlineStatus);
        editor.putString("taskName", taskName);
        editor.putBoolean("funMode", funMode);
        editor.putInt("startlinesMissed", startlinesMissed);
        editor.putBoolean("musicMode", musicMode);

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
            startlinesMissed = sharedPreferences.getInt("startlinesMissed", 0);
            boolean musicMode = sharedPreferences.getBoolean("musicMode", true);

            TextView startlineStatusTextView = findViewById(R.id.startline_status);
            TextView funlineStatusTextView = findViewById(R.id.funline_status);
            TextInputEditText taskNameTextView = findViewById(R.id.task_name);
            Switch funModeSwitch = findViewById(R.id.fun_mode_switch);
            Switch musicModeSwitch = findViewById(R.id.music_mode_switch);


            startlineStatusTextView.setText(startlineStatus);
            funlineStatusTextView.setText(funlineStatus);
            taskNameTextView.setText(taskName);
            funModeSwitch.setChecked(funMode);
            musicModeSwitch.setChecked(musicMode);
        } catch (Exception e) {
            saveStatuses("0", "0", "", false, true);
            loadStatuses();
        }
    }

    public void getAndSaveStatuses() {
        /* Get and Save Startlines, Funline, and Task Name */
        TextView startlineStatusTextView = findViewById(R.id.startline_status);
        TextView funlineStatusTextView = findViewById(R.id.funline_status);
        TextInputEditText taskNameTextView = findViewById(R.id.task_name);
        Switch funModeSwitch = findViewById(R.id.fun_mode_switch);
        Switch musicModeSwitch = findViewById(R.id.music_mode_switch);

        String startlineStatus = startlineStatusTextView.getText().toString();
        String funlineStatus = funlineStatusTextView.getText().toString();
        String taskName = taskNameTextView.getText().toString();
        boolean funMode = funModeSwitch.isChecked();
        boolean musicMode = musicModeSwitch.isChecked();

        saveStatuses(startlineStatus, funlineStatus, taskName, funMode, musicMode);
    }

    private void saveWorkingStatus() {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("workingStatus", workingStatus);
        editor.apply();
    }

    public boolean isFunModeOn() {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return prefs.getBoolean("funMode", false);
    }

    public boolean isWorking() {
        return workingStatus;
    }

    public boolean isInBlockingMode() {
        /* Used to determine when to start blocking apps

        Blocking mode is on if:
            Both Startlines and Funline are X
            One of them is X and the other is 0, and one startline has been missed
            One of them is X and the other is 1, and two startlines have been missed
        Blocking mode is off if:
            Neither are X
         */

        String startlineStatus = getStartlineStatus();
        String funlineStatus = getFunlineStatus();

        if (!startlineStatus.equals("X") && !funlineStatus.equals("X")) {
            return false;
        }

        boolean bothAreX = startlineStatus.equals("X") && funlineStatus.equals("X");
        boolean oneIsXAndOneIs0 = (startlineStatus.equals("X") && funlineStatus.equals("0")) || (startlineStatus.equals("0") && funlineStatus.equals("X"));
        boolean oneIsXAndOneIs1 = (startlineStatus.equals("X") && funlineStatus.equals("1")) || (startlineStatus.equals("1") && funlineStatus.equals("X"));

        return bothAreX || (oneIsXAndOneIs0 && startlinesMissed >= 1) || (oneIsXAndOneIs1 && startlinesMissed >= 2);
    }

    private void setWorkingStatusToTrue() {
        workingStatus = true;
        saveWorkingStatus();
    }

    private void setWorkingStatusToFalse() {
        workingStatus = false;
        saveWorkingStatus();
    }

    public boolean isTimeLimitPassed() {
        return System.currentTimeMillis() >= timeLimitInMillis;
    }


    public void addTextChangeListener() {
        TextInputEditText taskNameEditText = findViewById(R.id.task_name);
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
        if (isWorking()) {
            Toast.makeText(this, "Timebox is already running.", Toast.LENGTH_SHORT).show();
            Log.w("Timebox", "Timebox already running, not starting a new one");
            return;
        }
        startTimebox(2);
    }

    public void startTimebox(int currentTimeboxDuration) {
        /* For starting the 2 minute staircase timeboxes when the start button is pressed */
        setWorkingStatusToTrue();
        int timeboxDurationInMillis = currentTimeboxDuration * 60 * 1000;
        long endTimeInMillis = timestampMinutesFromNow(currentTimeboxDuration);
        StartlinesManager.sendStartlineMessageToServer(this);


        String endTimeFormatted = timestampToText(endTimeInMillis);
        NotificationHelper.showTimerNotification(this, currentTimeboxDuration, endTimeFormatted, !isFunModeOn());

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
            killTimeboxHandler();
            vibrateOnStop();
            setTimeboxStatusText("0");
            resetWorkingUntilTime();
            stopTickingSound();
            setWorkingStatusToFalse();
            StartlinesManager.sendStartlineMessageToServer(this);
            switchMusicModeToOn();
            updatePermanentNotification();
            NotificationHelper.cancelTimerNotification(this);
            Log.d("Timebox", "Timebox stopped");
        } else {
            Toast.makeText(this, "Timebox is not running. Nothing to stop.", Toast.LENGTH_SHORT).show();
            Log.w("Timebox", "Timebox is not running. Nothing to stop.");
        }
    }

    public void snooze() {
        if (getStartlineStatus() == "X") {
            startlineSnoozer = () -> {
                setStartlineStatus("1");
            };
        } else if (getFunlineStatus() == "X") {
            startlineSnoozer = () -> {
                setFunlineStatus("1");
            };
        }
        long twoMinutes = 2 * 60 * 1000;
        handler.postDelayed(startlineSnoozer, twoMinutes);
    }

    private void switchMusicModeToOn() {
        Switch musicModeSwitch = findViewById(R.id.music_mode_switch);
        if (!musicModeSwitch.isChecked()) {
            musicModeSwitch.setChecked(true);
        }
    }

    private void switchFunModeToOn(boolean funMode) {
        Switch funModeSwitch = findViewById(R.id.fun_mode_switch);
        if (!funModeSwitch.isChecked()) {
            funModeSwitch.setChecked(funMode);
        }

    }

    private void vibrateOnStop() {
        if (vibrator == null) {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }

        long[] vibrationPattern = {0, 150, 100, 150, 50, 200, 50, 500};
        VibrationEffect vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, -1); // -1 means do not repeat
        vibrator.vibrate(vibrationEffect);
    }

    private void showSetTimeLimitDialog() {
        /* Allow users to set a time limit for their timebox using a dialog box */
        if (!isWorking()) {
            // flash a toast message
            Toast.makeText(this, "Timer is not running, cannot set time limit.", Toast.LENGTH_SHORT).show();
            Log.w("Timebox", "Timebox is not running, cannot set time limit");
            return;
        }
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

    private void incrementStartlinesMissed() {
        startlinesMissed++;
        getAndSaveStatuses();
        Log.d("MainActivity", "Startlines missed: " + startlinesMissed);
    }

    private void resetStartlinesMissed() {
        startlinesMissed = 0;
        getAndSaveStatuses();
        Log.d("MainActivity", "Startlines missed reset");
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
        resetStartlinesMissed();
        NotificationHelper.cancelXModeNotification(this);
    }

    public void executeStartline(String lineType) {
        /* Checks Startline status and sets it accordingly */
        Log.d("MainActivity", "Executing Startline: " + lineType);
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        String status = prefs.getString(lineType + "Status", "0");

        if (status.equals("0") || status.equals("X")) {
            setLineStatus(lineType, "X");
            incrementStartlinesMissed();
            scheduleStartlineChecker(5, lineType);
            if (!isWorking()) {
                NotificationHelper.showXModeNotification(this, lineType.equalsIgnoreCase("startline"));
            }
        } else if (status.equals("1")) {
            setLineStatus(lineType, "0");
            resetStartlinesMissed();
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

        Log.d("Scheduler", "Alarm scheduled for: " + lineType + " at " + triggerAtMillis);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        Log.d("Scheduler", "Startline Checker scheduled for " + lineType + " in " + intervalInMinutes + " minutes");
    }

    private void playTickingSound() {
        if (tickingMediaPlayer == null) {
            tickingMediaPlayer = MediaPlayer.create(this, R.raw.clock_tick);
            tickingMediaPlayer.setLooping(true);
        }
        StartlinesManager.setTickingSoundPlaying(this, true);
        tickingMediaPlayer.start();
    }

    private void stopTickingSound() {
        if (tickingMediaPlayer != null) {
            tickingMediaPlayer.stop();
            tickingMediaPlayer.reset();  // Reset the media player
            tickingMediaPlayer.release();
            tickingMediaPlayer = null;
            StartlinesManager.setTickingSoundPlaying(this, false);
        }
    }

    private void killTimeboxHandler() {
        if (timeboxRunnable != null) {
            handler.removeCallbacks(timeboxRunnable);
        }
    }

    public String getStartlineStatus() {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return prefs.getString("startlineStatus", "0");
    }

    public String getFunlineStatus() {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        return prefs.getString("funlineStatus", "0");
    }

    public boolean isStartlinesXed() {
        return getStartlineStatus().equals("X");
    }

    public boolean isFunlineXed() {
        return getFunlineStatus().equals("X");
    }

    public boolean isStartlinesComplete() {
        return getStartlineStatus().equals("1");
    }

    public boolean isFunlineComplete() {
        return getFunlineStatus().equals("1");
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + AppBlockingAccessiblityService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            String enabledServices = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (enabledServices != null) {
                splitter.setString(enabledServices);
                while (splitter.hasNext()) {
                    String serviceName = splitter.next();
                    if (serviceName.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void showAccessibilityServiceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Accessibility Service")
                .setMessage("To block distracting apps, please enable accessibility service for Startlines.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    openAccessibilitySettings();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    private void openIPSettingsDialog() {
        // Fetch saved IP and port from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        String savedIp = prefs.getString("serverIp", "192.168.1.1");
        int savedPort = prefs.getInt("serverPort", 12345);

        // Create an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("IP and Port Settings");

        // Create a custom layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Add an EditText for IP
        EditText ipInput = new EditText(this);
        ipInput.setHint("Server IP Address");
        ipInput.setText(savedIp);
        layout.addView(ipInput);

        // Add an EditText for Port
        EditText portInput = new EditText(this);
        portInput.setHint("Server Port");
        portInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        portInput.setText(String.valueOf(savedPort));
        layout.addView(portInput);

        builder.setView(layout);

        // Save the new values when "OK" is clicked
        builder.setPositiveButton("Save", (dialog, which) -> {
            String ip = ipInput.getText().toString();
            int port = Integer.parseInt(portInput.getText().toString());

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("serverIp", ip);
            editor.putInt("serverPort", port);
            editor.apply();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
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