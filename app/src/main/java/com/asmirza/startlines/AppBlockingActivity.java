package com.asmirza.startlines;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppBlockingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_blocking);

        RecyclerView recyclerView = findViewById(R.id.app_list_recyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));

        List<AppInfo> appList = getInstalledApps();
        AppRecyclerViewAdapter adapter = new AppRecyclerViewAdapter(this, appList);
        recyclerView.setAdapter(adapter);
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
}