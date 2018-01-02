package com.example.frank.locationmind;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

import static android.content.Context.ALARM_SERVICE;

public class RebootGeoFenceReceiver extends BroadcastReceiver {
    private static final String GEOFENCE_REBOOT_ACTION = "com.example.frank.location.reboot.service";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if (intent.getAction().equals(GEOFENCE_REBOOT_ACTION)) {
            if (!isMyServiceRunning(context, com.example.frank.locationmind.GeoFenceService.class)) {
                Log.i("reboot receiver", "在广播接收器中重新启动服务");
                Intent it = new Intent(context, GeoFenceService.class);
                it.putExtra("FROM", "REBOOT_RCV");
                context.startService(it);
                //}else {//如果还在运行怎么办
                //等待5分钟后再次发送广播
                //setAlarmForService(context);
            }
        }
    }

    //本方法判断自己些的一个Service-->com.android.controlAddFunctions.PhoneService是否已经运行
    private boolean isMyServiceRunning(Context context,Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void setAlarmForService(Context context){
        AlarmManager manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        int threeMins = 1000*60*1; // 这是3分钟的毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime() + threeMins;

        Intent i = new Intent(context, RebootGeoFenceReceiver.class);
        i.setAction(GEOFENCE_REBOOT_ACTION);
        i.putExtra("FROM","ALARMREBOOT");
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
    }
}
