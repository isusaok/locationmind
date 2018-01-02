package com.example.frank.locationmind;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by frank on 17/12/23.
 */

public class BootCompleteStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            //example:启动程序
            Intent start = new Intent(context, GeoFenceService.class);
            start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//
            context.startActivity(start);
        }
    }
}
