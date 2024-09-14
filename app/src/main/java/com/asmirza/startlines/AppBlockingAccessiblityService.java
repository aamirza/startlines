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
        StartlinesManager.blockAppIfNecessary(this, packageName);
    }

    @Override
    public void onInterrupt() {

    }
}
