package com.example.frank.locationmind

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by frank on 17/12/25.
 */

class StopGeoFenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val i = Intent(context, GeoFenceService::class.java)
        context.stopService(i)
    }
}
