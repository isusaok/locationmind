package com.example.frank.locationmind;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by frank on 17/12/25.
 */

public class StopGeoFenceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context,GeoFenceService.class);
        context.stopService(i);
    }
}
