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
        if (StartlinesManager.isAppBlocked(this, packageName)) {
            Log.d("AppBlockingAccessiblityService", "Blocked app detected: " + packageName);
            StartlinesManager.blockApp(this, packageName);
        }
    }

    @Override
    public void onInterrupt() {

    }
}
