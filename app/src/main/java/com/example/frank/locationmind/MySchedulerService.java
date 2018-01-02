package com.example.frank.locationmind;

import android.app.ActivityManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by frank on 18/1/2.
 */

public class MySchedulerService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i("CHECKING","SCHEDULER 正常运行");
        if (!isMyServiceRunning(this,com.example.frank.locationmind.GeoFenceService.class)){
            Intent intentToService = new Intent(this, GeoFenceService.class);
            intentToService.putExtra("FROM","SCHEDULE");
            Log.i("CHECKING","SCHEDULER 运行中");
            startService(intentToService);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    //本方法判断自己些的一个Service-->com.android.controlAddFunctions.PhoneService是否已经运行
    private boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
