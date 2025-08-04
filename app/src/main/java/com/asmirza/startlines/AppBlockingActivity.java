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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppBlockingActivity extends AppCompatActivity {
    private AppRecyclerViewAdapter adapter;
    private String blockType;

    public static final String DISTRACTING_APPS_BLOCK = "DISTRACTING_APPS_BLOCK";
    public static final String X_MODE_BLOCK = "X_MODE_BLOCK";
    public static final String MUSIC_APPS = "MUSIC_APPS";
    public static final String CALENDAR_APP = "CALENDAR_APP";
    public static final String TEMPORARY_UNBLOCK = "TEMPORARY_UNBLOCK";
    public static final String ESSAY_APP = "ESSAY_APP";

    public static Map<String, String> BLOCK_TYPE = new HashMap<>();
    static {
        BLOCK_TYPE.put(X_MODE_BLOCK, "blockedApps");
        BLOCK_TYPE.put(DISTRACTING_APPS_BLOCK, "distractingApps");
        BLOCK_TYPE.put(MUSIC_APPS, "musicApps");
        BLOCK_TYPE.put(CALENDAR_APP, "calendarApp");
        BLOCK_TYPE.put(TEMPORARY_UNBLOCK, "temporarilyUnblockedApps");
        BLOCK_TYPE.put(ESSAY_APP, "essayApp");
        BLOCK_TYPE = Collections.unmodifiableMap(BLOCK_TYPE);
    }

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

        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
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

        editor.putStringSet(BLOCK_TYPE.get(blockType), selectedAppPackageNames);

        editor.apply();
    }

    public Set<String> loadBlockedApps() {
        SharedPreferences prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        String key = BLOCK_TYPE.get(blockType);
        if (key != null) {
            return prefs.getStringSet(BLOCK_TYPE.get(blockType), new HashSet<>());
        } else {
            return prefs.getStringSet("blockedApps", new HashSet<>());
        }
    }
}