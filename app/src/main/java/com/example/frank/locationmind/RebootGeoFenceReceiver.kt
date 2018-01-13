package com.example.frank.locationmind

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log

import java.util.ArrayList

import android.content.Context.ALARM_SERVICE

class RebootGeoFenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if (intent.action == GEOFENCE_REBOOT_ACTION) {
            if (!isMyServiceRunning(context, com.example.frank.locationmind.GeoFenceService::class.java)) {
                Log.i("reboot receiver", "在广播接收器中重新启动服务")
                val it = Intent(context, GeoFenceService::class.java)
                it.putExtra("FROM", "REBOOT_RCV")
                context.startService(it)
                //}else {//如果还在运行怎么办
                //等待5分钟后再次发送广播
                //setAlarmForService(context);
            }
        }
    }

    //本方法判断自己些的一个Service-->com.android.controlAddFunctions.PhoneService是否已经运行
    private fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun setAlarmForService(context: Context) {
        val manager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        val threeMins = 1000 * 60 * 1 // 这是3分钟的毫秒数
        val triggerAtTime = SystemClock.elapsedRealtime() + threeMins

        val i = Intent(context, RebootGeoFenceReceiver::class.java)
        i.action = GEOFENCE_REBOOT_ACTION
        i.putExtra("FROM", "ALARMREBOOT")
        val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT)
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi)
    }

    companion object {
        private val GEOFENCE_REBOOT_ACTION = "com.example.frank.location.reboot.service"
    }
}
