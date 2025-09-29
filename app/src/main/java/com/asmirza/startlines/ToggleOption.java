package com.asmirza.startlines;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public enum ToggleOption {
    BREAK("Break mode", "breakModeEnabled"),
    PREPARATION("Preparation mode", "preparationModeEnabled"),
    TASK("Task mode", "taskModeEenabled");

    private String modeName;
    private String prefKey;

    ToggleOption(String modeName, String prefKey) {
        this.modeName = modeName;
        this.prefKey = prefKey;
    }

    public String key() {
        return this.prefKey;
    }

    public String modeName() {
        return this.modeName;
    }

    public void toggleMode(Context context) {
        boolean isModeEnabled = isModeEnabled(context);
        setModeEnabled(context, !isModeEnabled);
        Toast.makeText(context, modeName + (isModeEnabled(context) ? " enabled." : " disabled."),
                Toast.LENGTH_SHORT).show();
    }

    public boolean isModeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean(this.key(), false);
    }

    public boolean setModeEnabled(Context context, boolean enable) {
        SharedPreferences prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(this.key(), enable);
        editor.apply();
        Log.d("ToggleManager", this.modeName() + " enabled: " + enable);
        return enable;
    }
}
