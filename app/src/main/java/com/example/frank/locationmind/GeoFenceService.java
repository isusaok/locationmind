package com.example.frank.locationmind;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.amap.api.fence.GeoFence;
import com.amap.api.fence.GeoFenceClient;
import com.amap.api.fence.GeoFenceListener;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.DPoint;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.amap.api.maps2d.AMapUtils.calculateLineDistance;
import static java.lang.System.out;

public class GeoFenceService extends Service
                                implements GeoFenceListener, AMapLocationListener {
    private List<GeoFence> fenceList = new ArrayList<GeoFence>();
    private ArrayList<Reminder> reminderList =new ArrayList<Reminder>();
    protected NotificationManager mNMgr;
    private  static final int NOTIFY_MSG = 000001;


    private GeoFenceClient mGeoFenceClient;
    private static final String GEOFENCE_BROADCAST_ACTION = "com.example.frank.location.geofence";
    private static final String GEOFENCE_REBOOT_ACTION = "com.example.frank.location.reboot.service";


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Message msg = Message.obtain();
        msg.what = 2;
        String som = intent.getStringExtra("FROM");
        msg.obj = som;
        handler.sendMessage(msg);
        Log.i("start service","启动服务来自于"+som);

        mNMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);


        //从文件中获取现有reminder list
        reminderList = readListFromFile("HELLO");
        Log.i("start service","inten接受数组的大小"+Integer.toString(reminderList.size()));

        //创建GeoFence
        setUpGeoFence();

        return super.onStartCommand(intent, flags, startId);
    }

    private void setUpGeoFence(){
        Log.i("start service","开始");
        mGeoFenceClient = new GeoFenceClient(getApplicationContext());
        Log.i("start service","地理围栏客户端初始化成功");

        //监听网络变化
        IntentFilter itFilt = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        //增加监听地理围栏
        itFilt.addAction(GEOFENCE_BROADCAST_ACTION);

        //注册广播接受器，也可以在Androidmanifest中注册
        registerReceiver(mGeoFenceReceiver,itFilt);
        mGeoFenceClient.createPendingIntent(GEOFENCE_BROADCAST_ACTION);
        mGeoFenceClient.setGeoFenceListener(this);
        mGeoFenceClient.setActivateAction(GeoFenceClient.GEOFENCE_IN|GeoFenceClient.GEOFENCE_OUT|GeoFenceClient.GEOFENCE_STAYED);
        createGeoFenceFromList(reminderList);
    }


    private void createGeoFenceFromList(ArrayList<Reminder> ls){
        if (null != ls) {
            for (int i=0;i<ls.size();i++) {
                Reminder re = ls.get(i);
                Log.i("create fence",re.toString());
                DPoint centPoint = new DPoint();
                centPoint.setLatitude(re.getLat());
                centPoint.setLongitude(re.getLng());
                String customeID = Integer.toString(i)+"_"+re.getTaskDescription();
                mGeoFenceClient.addGeoFence(centPoint, 500F, customeID);
            }
            Log.i("create fence list","从list中加入围栏");
        }else{
            Log.i("create fence list","list为空，不加入围栏");
        }
    }

    //围栏出发的消息接受方,直接在服务内部处理消息，发送通知
    private BroadcastReceiver mGeoFenceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("GeoFence RCV","广播接收到了");
            // 接收广播
            if (intent.getAction().equals(GEOFENCE_BROADCAST_ACTION)) {
                Bundle bundle = intent.getExtras();
                String customId = bundle
                        .getString(GeoFence.BUNDLE_KEY_CUSTOMID);
                //customerID to reminder
                int a =0;
                try {
                    a = Integer.parseInt(customId.substring(0,customId.indexOf("_")));
                    //Log.i("GeoFence RCV",Integer.toString(a));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                Reminder currentReminder = reminderList.get(a);


                String fenceId = bundle.getString(GeoFence.BUNDLE_KEY_FENCEID);
                //status标识的是当前的围栏状态，不是围栏行为
                int status = bundle.getInt(GeoFence.BUNDLE_KEY_FENCESTATUS);
                StringBuffer sb = new StringBuffer();
                switch (status) {
                    case GeoFence.STATUS_LOCFAIL :
                        sb.append("定位失败");
                        break;
                    case GeoFence.STATUS_IN :
                        sb.append("进入围栏 ");
                        sb.append(customId);
                        if (currentReminder.ReminderType.contains(Reminder.LocationState.GEO_IN_REMINDIE))
                            sendAnNotification(sb.toString());
                        break;
                    case GeoFence.STATUS_OUT :
                        sb.append("离开围栏 ");
                        sb.append(customId);
                        if (currentReminder.ReminderType.contains(Reminder.LocationState.GEO_OUT_REMINDIE))
                            sendAnNotification(sb.toString());
                        break;
                    case GeoFence.STATUS_STAYED :
                        sb.append("停留在围栏内 ");
                        sb.append(customId);
                        if (currentReminder.ReminderType.contains(Reminder.LocationState.GEO_STAY_REMINDIE))
                            sendAnNotification(sb.toString());
                        break;
                    default :
                        break;
                }
                if(status != GeoFence.STATUS_LOCFAIL){
                    sb.append(" fenceId: " + fenceId);
                }
                String str = sb.toString();
                Message msg = Message.obtain();
                msg.obj = str;
                msg.what = 2;
                handler.sendMessage(msg);
            }
        }
    };


    public void setAlarmForService(int timeRangeInSeconds){
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int threeMins = 1000*60*timeRangeInSeconds; // 这是3分钟的毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime() + threeMins;

        Intent i = new Intent(this, RebootGeoFenceReceiver.class);
        i.setAction(GEOFENCE_REBOOT_ACTION);
        i.putExtra("FROM","ALARMREBOOT");
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
    }

    //判断用户是否需要停止服务
    private boolean isServiceManageStopped(){
        SharedPreferences mSharedPreferences = getSharedPreferences("REMINDER_LIST_STATES", Context.MODE_PRIVATE);
        boolean bl = false;
        return mSharedPreferences.getBoolean("SERVICE_MANAGE_STOPPED",bl);
    }

    @Override
    public void onGeoFenceCreateFinished(List<GeoFence> list, int errorCode, String customId) {
        Log.i("on GeoFence","finished");
        Message msg = Message.obtain();
        if (errorCode == GeoFence.ADDGEOFENCE_SUCCESS) {
            for(GeoFence gf :list){
                fenceList.add(gf);
                Log.i("after geofence created","添加GEO");
            }
            msg.obj = customId;
            msg.what = 0;
        } else {
            msg.arg1 = errorCode;
            msg.what = 1;
        }
        handler.sendMessage(msg);
        int templistlen = mGeoFenceClient.getAllGeoFence().size();
        Log.i("after geofence created",Integer.toString(templistlen));
    }

    //创建围栏的消息传递机制，handle负责
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0 ://围栏添加成功事件
                    StringBuilder sb = new StringBuilder();
                    sb.append("添加围栏成功");
                    String customId = (String)msg.obj;
                    Toast.makeText(getApplicationContext(), sb.toString(),
                            Toast.LENGTH_SHORT).show();
                    break;
                case 1 ://围栏添加失败事件
                    int errorCode = msg.arg1;
                    Toast.makeText(getApplicationContext(),
                            "添加围栏失败 " + errorCode, Toast.LENGTH_SHORT).show();
                    break;
                case 2 ://围栏触发事件
                    String statusStr = (String) msg.obj;
                    Toast.makeText(getApplicationContext(),
                            "围栏事件 " + statusStr, Toast.LENGTH_SHORT).show();
                    break;
                default :
                    break;
            }
        }
    };


    private boolean checkGeoFence(){
        return true;
    }

    public void sendAnNotification(String str) {
        int NOTIFICATION_ID_LOCATION;
        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentTitle("some messages");
        builder.setContentText("content text"+str);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setWhen(System.currentTimeMillis());

        Intent intent = new Intent(getBaseContext(),MainActivity.class);
        PendingIntent pdin = PendingIntent.getActivity(getBaseContext(),0,intent,0);
        builder.setContentIntent(pdin);

        Notification noti = builder.build();
        mNMgr.notify(NOTIFY_MSG,noti);
    }


    //在服务即将销毁之前设置3分钟之后再次启动服务
    @Override
    public void onDestroy() {
        super.onDestroy();
        //定位服务清除
        /*
        if(null != aMapLocationClient){
            aMapLocationClient.onDestroy();
        }
        */
        //地理围栏清除
        mGeoFenceClient.removeGeoFence();
        //围栏监听服务清除
        this.unregisterReceiver(mGeoFenceReceiver);
        //围栏清除
        fenceList.clear();
        //5分钟后重新启动
        //setAlarmForService(5);
        Toast.makeText(this, "服务已经停止", Toast.LENGTH_LONG).show();
    }

    //对象数组存入文件
    public void writeListIntoFile(String fileName,ArrayList stus){
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream( new BufferedOutputStream(openFileOutput(fileName,Context.MODE_PRIVATE)));
            oos.writeObject(stus);
            oos.flush();
            oos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally{
            out.println( "文件写入完成!" );
        }
    }

    //从文件读取数组
    public <T> ArrayList<T> readListFromFile(String fileName){
        ArrayList<T> arrayList = new ArrayList<>();
        try {
            ObjectInputStream ois = new ObjectInputStream( new BufferedInputStream(openFileInput(fileName )));
            arrayList=(ArrayList<T>) ois.readObject();
            ois.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally{
            out.println( "读取成功!");
        }
        return arrayList;
    }


    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if(aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0){
                /*
                LatLng mLatlng = new LatLng(aMapLocation.getLatitude(),aMapLocation.getLongitude());
                float mSpeed = aMapLocation.getSpeed();//m/s
                Double maxSpeed = (mSpeed>8.3)?mSpeed:8.3;//8.3 automobile speed in city

                double aaa = distanceToNearestReminder(mLatlng);
                Double nextTime = aaa/maxSpeed;
                Log.i("onlocationchanged", "围栏数量"+Integer.toString(fenceList.size()));

                if (nextTime<10*60){ //离围栏很近
                    Log.i("onlocationchanged", "距离很近");
                    if (fenceList.size()<1) {
                        setUpGeoFence();//设置围栏
                    }
                }else {//离围栏很远
                    Log.i("onlocationchanged", "距离很远");

                    if(null!=mGeoFenceClient){
                       mGeoFenceClient.removeGeoFence();//删除围栏
                        fenceList.clear();
                    }
                }
                */
                Log.i("onlocationchanged", "success");
            } else {
                Log.e("AmapError","location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }

    }

    private float distanceToNearestReminder(LatLng mLatLng){
        float minDistance = 1000000;
        for (int i = 0;i<reminderList.size();i++){
            Reminder rd = reminderList.get(i);
            float presentDistance = AMapUtils.calculateLineDistance(mLatLng,new LatLng(rd.getLat(),rd.getLng()));
            Log.i("onlocationcaculat","围栏"+Integer.toString(i)+"距离："+Float.toString(presentDistance));
            if (presentDistance < minDistance){
                minDistance = presentDistance;
            }
        }
        return minDistance;
    }

}
