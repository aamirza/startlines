package com.asmirza.startlines;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;


public class AppBlockingAccessiblityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.v("AppBlockingAccessiblityService", "Accessibility event received");
        String packageName = event.getPackageName().toString();
        Log.v("AppBlockingAccessiblityService", "Package name: " + packageName);
        StartlinesManager.blockAppIfNecessary(this, packageName);
    }

    @Override
    public void onInterrupt() {

    }
}
