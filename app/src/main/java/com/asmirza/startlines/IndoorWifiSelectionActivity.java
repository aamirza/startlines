package com.asmirza.startlines;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IndoorWifiSelectionActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private WifiManager wifiManager;
    private WifiAdapter wifiAdapter;
    private List<String> wifiList = new ArrayList<>();
    private Set<String> selectedWifiSet = new HashSet<>();
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_selection);

        sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);

        // Load previously saved indoor WiFi networks
        selectedWifiSet = sharedPreferences.getStringSet("indoorWiFi", new HashSet<>());

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        RecyclerView recyclerView = findViewById(R.id.wifiRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        wifiAdapter = new WifiAdapter(wifiList, selectedWifiSet);
        recyclerView.setAdapter(wifiAdapter);

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> saveSelectedWifi());

        checkPermissionsAndScan();
    }

    private void checkPermissionsAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            scanWifiNetworks();
        }
    }

    private void scanWifiNetworks() {
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi is disabled. Enable it to scan networks.", Toast.LENGTH_SHORT).show();
            return;
        }

        wifiManager.startScan();
        List<ScanResult> scanResults = wifiManager.getScanResults();
        wifiList.clear();

        for (ScanResult scanResult : scanResults) {
            wifiList.add(scanResult.SSID);
        }

        wifiAdapter.notifyDataSetChanged();
    }

    private void saveSelectedWifi() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("indoorWiFi", wifiAdapter.getSelectedWifi());
        editor.apply();
        Toast.makeText(this, "Indoor WiFi saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanWifiNetworks();
            } else {
                Toast.makeText(this, "Location permission is required to scan WiFi networks.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
