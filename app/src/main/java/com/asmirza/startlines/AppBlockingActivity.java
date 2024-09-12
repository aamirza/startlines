package com.asmirza.startlines;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppBlockingActivity extends AppCompatActivity {
    private AppRecyclerViewAdapter adapter;
    private String blockType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_blocking);

        blockType = getIntent().getStringExtra("blockType");
        Set<String> previouslySelectedAppPackageNames = loadBlockedApps();

        RecyclerView recyclerView = findViewById(R.id.app_list_recyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));

        List<AppInfo> appList = getInstalledApps();
        adapter = new AppRecyclerViewAdapter(this, appList, previouslySelectedAppPackageNames);
        recyclerView.setAdapter(adapter);

        Button saveButton = findViewById(R.id.save_changes_button);
        saveButton.setOnClickListener(v -> {
            saveBlockedApps();
            finish();
        });
    }

    private List<AppInfo> getInstalledApps() {
        List<AppInfo> appList = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> sortedAppList = sortAppList(apps);

        for (ApplicationInfo app : sortedAppList) {
            if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                String appName = packageManager.getApplicationLabel(app).toString();
                Drawable appIcon = packageManager.getApplicationIcon(app);
                String packageName = app.packageName;
                appList.add(new AppInfo(appName, appIcon, packageName));
            }
        }

        return appList;
    }

    // sort appList by appName alphabetically
    private List<ApplicationInfo> sortAppList(List<ApplicationInfo> appList) {
        Collections.sort(appList, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo app1, ApplicationInfo app2) {
                PackageManager packageManager = getPackageManager();
                String appName1 = packageManager.getApplicationLabel(app1).toString();
                String appName2 = packageManager.getApplicationLabel(app2).toString();
                return appName1.compareToIgnoreCase(appName2);
            }
        });

        return appList;
    }

    private void saveBlockedApps() {
        /* For saving and persisting the list of apps that were selected for blocking */
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> selectedAppPackageNames = new HashSet<>();
        for (AppInfo appInfo : adapter.getSelectedApps()) {
            selectedAppPackageNames.add(appInfo.getPackageName());
        }

        if (blockType.equals("X_MODE_BLOCK")) {
            editor.putStringSet("blockedApps", selectedAppPackageNames);
        } else if (blockType.equals("DISTRACTING_APPS_BLOCK")) {
            editor.putStringSet("distractingApps", selectedAppPackageNames);
        }

        editor.apply();
    }

    public Set<String> loadBlockedApps() {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        if (blockType.equals("X_MODE_BLOCK")) {
            return prefs.getStringSet("blockedApps", new HashSet<>());
        } else if (blockType.equals("DISTRACTING_APPS_BLOCK")) {
            return prefs.getStringSet("distractingApps", new HashSet<>());
        }

        return prefs.getStringSet("blockedApps", new HashSet<>());
    }
}