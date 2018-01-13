package com.example.frank.locationmind

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by frank on 17/12/23.
 */

class BootCompleteStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            //example:启动程序
            val start = Intent(context, GeoFenceService::class.java)
            start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)//
            context.startActivity(start)
        }
    }
}
