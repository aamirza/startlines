package com.asmirza.startlines;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartlineCheckerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String lineType = intent.getStringExtra("lineType");

        MainActivity mainActivity = new MainActivity();
        mainActivity.executeStartline(lineType);
        mainActivity.scheduleStartline();
    }
}
