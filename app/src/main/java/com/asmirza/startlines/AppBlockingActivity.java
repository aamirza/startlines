package com.asmirza.startlines;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import java.util.ArrayList;
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

        for (ApplicationInfo app : apps) {
            if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                String appName = packageManager.getApplicationLabel(app).toString();
                Drawable appIcon = packageManager.getApplicationIcon(app);
                String packageName = app.packageName;
                appList.add(new AppInfo(appName, appIcon, packageName));
            }
        }

        return appList;
    }
}