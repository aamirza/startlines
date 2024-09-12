package com.asmirza.startlines;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;


public class AppBlockingAccessiblityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d("AppBlockingAccessiblityService", "Accessibility event received");
        String packageName = event.getPackageName().toString();
        Log.d("AppBlockingAccessiblityService", "Package name: " + packageName);
        boolean working = StartlinesManager.isTimeboxRunning(this);
        if (working && StartlinesManager.isAppDistracting(this, packageName)) {
            Log.d("AppBlockingAccessiblityService", "Distracting app detected: " + packageName);
            StartlinesManager.blockDistractingApp(this, packageName);
        } else if (!working && StartlinesManager.isAppBlocked(this, packageName)) {
            Log.d("AppBlockingAccessiblityService", "Blocked app detected: " + packageName);
            StartlinesManager.blockApp(this, packageName);
        }
    }

    @Override
    public void onInterrupt() {

    }
}
