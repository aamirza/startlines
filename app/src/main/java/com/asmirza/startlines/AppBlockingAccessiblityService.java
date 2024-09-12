package com.asmirza.startlines;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

public class AppBlockingAccessiblityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d("AppBlockingAccessiblityService", "Accessibility event received");
        String packageName = event.getPackageName().toString();
        Log.d("AppBlockingAccessiblityService", "Package name: " + packageName);
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        Set<String> blockedApps = sharedPreferences.getStringSet("blockedApps", new HashSet<>());
        if (blockedApps.contains(packageName)) {
            Log.d("AppBlockingAccessiblityService", "Blocked app detected: " + packageName);
            blockApp(packageName);
        }
    }

    private boolean isInAppBlockingMode() {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
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

    private void blockApp(String packageName) {
        if (isInAppBlockingMode()) {
            Log.d("AppBlockingAccessiblityService", "Blocking app: " + packageName);
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onInterrupt() {

    }
}
