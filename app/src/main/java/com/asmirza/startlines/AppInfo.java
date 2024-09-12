package com.asmirza.startlines;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private String appName;
    private Drawable appIcon;
    private String packageName;

    public AppInfo(String appName, Drawable appIcon, String packageName) {
        this.appName = appName;
        this.appIcon = appIcon;
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public String getPackageName() {
        return packageName;
    }
}
