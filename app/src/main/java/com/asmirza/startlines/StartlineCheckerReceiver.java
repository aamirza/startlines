package com.asmirza.startlines;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartlineCheckerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String lineType = intent.getStringExtra("lineType");
        Log.d("StartlineCheckerReceiver", "StartlineCheckerReceiver entered with lineType: " + lineType);
        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        mainActivityIntent.putExtra("lineType", lineType);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(mainActivityIntent);
    }
}
