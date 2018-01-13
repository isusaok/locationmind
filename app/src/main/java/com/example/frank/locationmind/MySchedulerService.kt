package com.example.frank.locationmind

import android.app.ActivityManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Created by frank on 18/1/2.
 */

class MySchedulerService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Log.i("CHECKING", "SCHEDULER 正常运行")
        if (!isMyServiceRunning(this, com.example.frank.locationmind.GeoFenceService::class.java)) {
            val intentToService = Intent(this, GeoFenceService::class.java)
            intentToService.putExtra("FROM", "SCHEDULE")
            Log.i("CHECKING", "SCHEDULER 运行中")
            startService(intentToService)
        }
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return false
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
}
