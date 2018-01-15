package com.example.frank.locationmind

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.SystemClock
import android.util.Log
import android.widget.Toast

import com.amap.api.fence.GeoFence
import com.amap.api.fence.GeoFenceClient
import com.amap.api.fence.GeoFenceListener
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationListener
import com.amap.api.location.DPoint

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

import java.lang.System.out
import java.util.*
import kotlin.collections.ArrayList as KArrayList

class GeoFenceService : Service(), GeoFenceListener, AMapLocationListener {
    private val fenceList = ArrayList<GeoFence>()
    private var reminderList = ArrayList<Reminder>()
    protected var mNMgr: NotificationManager?=null
    private val fenceCreatedTimeList = ArrayList<Long>(Collections.nCopies(50,0L))

    private var mGeoFenceClient: GeoFenceClient? = null

    //围栏出发的消息接受方,直接在服务内部处理消息，发送通知
    private val mGeoFenceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i("GeoFence RCV", "广播接收到了")
            // 接收广播
            if (intent.action == GEOFENCE_BROADCAST_ACTION) {
                val bundle = intent.extras
                val customId = bundle!!
                        .getString(GeoFence.BUNDLE_KEY_CUSTOMID)
                //customerID to reminder
                var a = 0
                try {
                    a = Integer.parseInt(customId!!.substring(0, customId.indexOf("_")))
                    //Log.i("GeoFence RCV",Integer.toString(a));
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }

                val currentReminder = reminderList[a]

                val fenceId = bundle.getString(GeoFence.BUNDLE_KEY_FENCEID)
                //status标识的是当前的围栏状态，不是围栏行为
                val status = bundle.getInt(GeoFence.BUNDLE_KEY_FENCESTATUS)
                val sb = StringBuffer()
                when (status) {
                    GeoFence.STATUS_LOCFAIL -> sb.append("定位失败")
                    GeoFence.STATUS_IN -> {
                        sb.append("进入围栏")
                        sb.append(a)
                        sb.append(":在")
                        sb.append(currentReminder.placeDescription)
                        sb.append(currentReminder.taskDescription)
                        //sb.append(customId);
                        if (currentReminder.ReminderType!!.contains(Reminder.LocationState.GEO_IN_REMINDIE)) {
                            checkTimeThenNotify(sb.toString(), a)
                        }
                    }
                    GeoFence.STATUS_OUT -> {
                        sb.append("离开围栏 ")
                        sb.append(a)
                        sb.append(":在")
                        sb.append(currentReminder.placeDescription)
                        sb.append(currentReminder.taskDescription)
                        if (currentReminder.ReminderType!!.contains(Reminder.LocationState.GEO_OUT_REMINDIE)) {
                            checkTimeThenNotify(sb.toString(), a)
                        }
                    }
                    GeoFence.STATUS_STAYED -> {
                        sb.append("停留在围栏内 ")
                        sb.append(a)
                        sb.append(":在")
                        sb.append(currentReminder.placeDescription)
                        sb.append(currentReminder.taskDescription)
                        if (currentReminder.ReminderType!!.contains(Reminder.LocationState.GEO_STAY_REMINDIE)) {
                            checkTimeThenNotify(sb.toString(), a)
                        }
                    }
                    else -> {
                    }
                }
                if (status != GeoFence.STATUS_LOCFAIL) {
                    sb.append(" fenceId: " + fenceId!!)
                }
                val str = sb.toString()
                val msg = Message.obtain()
                msg.obj = str
                msg.what = 2
                handler.sendMessage(msg)
            }
        }
    }

    //check time，after geofence created 1 minute then start notification,阻止地理围栏创建的的第一次广播
    private fun checkTimeThenNotify(str: String, position: Int){
        val currentMillis = System.currentTimeMillis()
        Log.i("creat time",position.toString()+"----------"+fenceCreatedTimeList[position].toString())
        Log.i("current time",currentMillis.toString())
        val millisDiff = System.currentTimeMillis()-fenceCreatedTimeList[position]
        Log.i("current time difference",millisDiff.toString())
        if ((millisDiff>1*60*1000) and (millisDiff<1000*60*60*24) ) {//地理围栏建立的时间在1天与1分钟之间通知，否则忽略
            sendAnNotification(str, position)
        }
    }

    //判断用户是否需要停止服务
    private val isServiceManageStopped: Boolean
        get() {
            val mSharedPreferences = getSharedPreferences("REMINDER_LIST_STATES", Context.MODE_PRIVATE)
            val bl = false
            return mSharedPreferences.getBoolean("SERVICE_MANAGE_STOPPED", bl)
        }

    //创建围栏的消息传递机制，handle负责
    @SuppressLint("HandlerLeak")
    internal var handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0//围栏添加成功事件
                -> {
                    val sb = StringBuilder()
                    sb.append("添加围栏成功")
                    //val customId = msg.obj as String
                    Toast.makeText(applicationContext, sb.toString(),
                            Toast.LENGTH_SHORT).show()
                }
                1//围栏添加失败事件
                -> {
                    val errorCode = msg.arg1
                    Toast.makeText(applicationContext,
                            "添加围栏失败 " + errorCode, Toast.LENGTH_SHORT).show()
                }
                2//围栏触发事件
                -> {
                }
                else -> {
                }
            }/*
                    String statusStr = (String) msg.obj;
                    Toast.makeText(getApplicationContext(),
                            "围栏事件 " + statusStr, Toast.LENGTH_SHORT).show();
                            */
        }
    }


    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val msg = Message.obtain()
        msg.what = 2
        val som = intent.getStringExtra("FROM")
        msg.obj = som
        handler.sendMessage(msg)
        Log.i("start service", "启动服务来自于" + som)

        mNMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        //从文件中获取现有reminder list
        reminderList = readListFromFile("HELLO")
        Log.i("start service", "inten接受数组的大小" + Integer.toString(reminderList.size))

        //创建GeoFence
        setUpGeoFence()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun setUpGeoFence() {
        mGeoFenceClient = GeoFenceClient(applicationContext)

        //监听网络变化
        val itFilt = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        //增加监听地理围栏
        itFilt.addAction(GEOFENCE_BROADCAST_ACTION)

        //注册广播接受器，也可以在Androidmanifest中注册
        registerReceiver(mGeoFenceReceiver, itFilt)
        mGeoFenceClient!!.createPendingIntent(GEOFENCE_BROADCAST_ACTION)
        mGeoFenceClient!!.setGeoFenceListener(this)
        mGeoFenceClient!!.setActivateAction(GeoFenceClient.GEOFENCE_IN or GeoFenceClient.GEOFENCE_OUT or GeoFenceClient.GEOFENCE_STAYED)
        createGeoFenceFromList(reminderList)
    }


    private fun createGeoFenceFromList(ls: ArrayList<Reminder>?) {
        if (null != ls) {
            for (i in ls.indices) {
                val re = ls[i]
                if (re.isWorking) {
                    Log.i("create fence", re.toString())
                    val centPoint = DPoint()
                    centPoint.latitude = re.lat
                    centPoint.longitude = re.lng
                    val customeID = Integer.toString(i) + "_" + re.taskDescription
                    mGeoFenceClient!!.addGeoFence(centPoint, if (re.diameter < 500) 500f else re.diameter.toFloat(), customeID)
                    fenceCreatedTimeList.add(i,System.currentTimeMillis())
                }
            }
            Log.i("create fence list", "从list中加入围栏")
        } else {
            Log.i("create fence list", "list为空，不加入围栏")
        }
    }

    //Alarm Service
    fun setAlarmForService(timeRangeInSeconds: Int) {
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val threeMins = 1000 * 60 * timeRangeInSeconds // 这是3分钟的毫秒数
        val triggerAtTime = SystemClock.elapsedRealtime() + threeMins

        val i = Intent(this, RebootGeoFenceReceiver::class.java)
        i.action = GEOFENCE_REBOOT_ACTION
        i.putExtra("FROM", "ALARMREBOOT")
        val pi = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT)
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi)
    }

    override fun onGeoFenceCreateFinished(list: List<GeoFence>, errorCode: Int, customId: String) {
        Log.i("on GeoFence", "finished")
        val msg = Message.obtain()
        if (errorCode == GeoFence.ADDGEOFENCE_SUCCESS) {
            for (gf in list) {
                fenceList.add(gf)
                Log.i("after geofence created", "添加GEO")
            }
            msg.obj = customId
            msg.what = 0
        } else {
            msg.arg1 = errorCode
            msg.what = 1
        }
        handler.sendMessage(msg)
        val templistlen = mGeoFenceClient!!.allGeoFence.size
        Log.i("after geofence created", Integer.toString(templistlen))
    }


    fun sendAnNotification(str: String, position: Int) {
        //val NOTIFICATION_ID_LOCATION: Int
        val builder = Notification.Builder(this)

        builder.setContentTitle("地理围栏")
        builder.setContentText(str)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setWhen(System.currentTimeMillis())

        val intent = Intent(baseContext, MainActivity::class.java)
        intent.action = "com.example.frank.location.notify.start"
        intent.putExtra("NOTIFIED_ITEM", position)

        val pdin = PendingIntent.getActivity(baseContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pdin)
        builder.setAutoCancel(true)

        val noti = builder.build()
        mNMgr!!.notify(NOTIFY_MSG, noti)
    }


    //在服务即将销毁之前设置3分钟之后再次启动服务
    override fun onDestroy() {
        super.onDestroy()
        //地理围栏清除
        mGeoFenceClient!!.removeGeoFence()
        //围栏监听服务清除
        this.unregisterReceiver(mGeoFenceReceiver)
        //围栏清除
        fenceList.clear()
        //5分钟后重新启动
        //setAlarmForService(5);
        Toast.makeText(this, "服务已经停止", Toast.LENGTH_LONG).show()
    }

    //对象数组存入文件
    fun writeListIntoFile(fileName: String, stus: ArrayList<*>) {
        val oos: ObjectOutputStream
        try {
            oos = ObjectOutputStream(BufferedOutputStream(openFileOutput(fileName, Context.MODE_PRIVATE)))
            oos.writeObject(stus)
            oos.flush()
            oos.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } finally {
            out.println("文件写入完成!")
        }
    }

    //从文件读取数组
    fun <T> readListFromFile(fileName: String): ArrayList<T> {
        var arrayList = ArrayList<T>()
        try {
            val ois = ObjectInputStream(BufferedInputStream(openFileInput(fileName)))
            arrayList = ois.readObject() as ArrayList<T>
            ois.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } finally {
            out.println("读取成功!")
        }
        return arrayList
    }


    override fun onLocationChanged(aMapLocation: AMapLocation?) {
        if (aMapLocation != null) {
            if (aMapLocation.errorCode == 0) {

                Log.i("onlocationchanged", "success")
            } else {
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.errorCode + ", errInfo:"
                        + aMapLocation.errorInfo)
            }
        }

    }

    companion object {
        private val NOTIFY_MSG = 1
        private val GEOFENCE_BROADCAST_ACTION = "com.example.frank.location.geofence"
        private val GEOFENCE_REBOOT_ACTION = "com.example.frank.location.reboot.service"
    }

}
