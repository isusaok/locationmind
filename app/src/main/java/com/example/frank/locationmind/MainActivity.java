package com.example.frank.locationmind;

import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.*;

public class MainActivity extends AppCompatActivity {

    private static final String dataFileURI = "HELLO";
    private static final int SCHEDULE_SERVICE_ID = 9001;
    private @Nullable ArrayList<Reminder> reminderList = new ArrayList<Reminder>();

    private RecyclerView mRecyclerView;
    private FloatingActionButton fabAdd,fabupdate;
    private Toolbar toolbar;

    private LocalBroadcastManager localBroadcastManager;
    private StopGeoFenceReceiver mStopGeoFenceRCV;

    MyQuickAdapter myAdapter;


    //返回按钮监听



    @Nullable
    public List<Reminder> getReminderList() {
        if(reminderList == null)
            reminderList = new ArrayList<Reminder>();
        return reminderList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setServiceManageStopped(false);
        //recyclerView setup
        mRecyclerView = (RecyclerView)findViewById(R.id.RCV_MAIN);

        //floating Action button setup
        fabAdd = (FloatingActionButton)findViewById(R.id.FAB_MAIN);
        fabupdate =(FloatingActionButton)findViewById(R.id.FAB1_MAIN);

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //增加提醒按钮
                Intent intentToAddReminderPage = new Intent(MainActivity.this, AddReminderActivity.class);
                startActivityForResult(intentToAddReminderPage,1500);
            }
        });

        fabupdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tempStr =isMyServiceRunning(MainActivity.this,com.example.frank.locationmind.GeoFenceService.class)?"OK":"FAIL";
                Log.i("通过Main启动",tempStr);
                Intent intentToService = new Intent(MainActivity.this,GeoFenceService.class);
                startService(intentToService);
            }
        });

        //toolbar setup
        toolbar= (Toolbar) findViewById(R.id.toolbar_V);
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                                               @Override
                                               public boolean onMenuItemClick(MenuItem item) {
                                                   switch (item.getItemId()){
                                                       case R.id.STOP_MENU:
                                                           if(isMyServiceRunning(MainActivity.this, com.example.frank.locationmind.GeoFenceService.class)){
                                                               Intent intent = new Intent("com.example.frank.locationmind.stop.servie");
                                                               localBroadcastManager.sendBroadcast(intent);
                                                               setServiceManageStopped(true);
                                                           }
                                                           break;
                                                       case R.id.UPDATE_MENU:
                                                           break;
                                                       case R.id.OTHER_MENU:
                                                           if(isMyServiceRunning(MainActivity.this, com.example.frank.locationmind.GeoFenceService.class)){
                                                               Intent intent = new Intent("com.example.frank.locationmind.stop.servie");
                                                               localBroadcastManager.sendBroadcast(intent);
                                                           }
                                                           break;
                                                       default:
                                                           break;
                                                   }
                                                   return true;
                                               }
                                           }
        );

        //设置适配器
        myAdapter = new MyQuickAdapter(R.layout.cardlayout,reminderList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(myAdapter);
        mRecyclerView.addItemDecoration(new SimplePaddingDecoration(this));

        //copy reminders.json from external storage Download dir to internal storage
        String o = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/reminders.json";
        String n = getFilesDir()+"/reminders.json";
        Log.i("file copy from",o);
        Log.i("file to",n);
        copyFileAn(o,n);

        //load data from reminders.json
        boolean jsaonFileLoaded = initReminderListFromJSONFILE("reminders.json");
        if (!jsaonFileLoaded){
            //if failed, load from asset file
            boolean isLoaded = initDataFromJSONAsset("reminders.json");
            if(isLoaded)
                Log.i("jason asset ","加载成功");
        }
        //setup data for Adapter


        writeListIntoFile("HELLO",reminderList);
        setServieNeedUpdated(true);

        localBroadcastManager= LocalBroadcastManager.getInstance(this);

        mStopGeoFenceRCV = new StopGeoFenceReceiver();
        localBroadcastManager.registerReceiver(mStopGeoFenceRCV,new IntentFilter("com.example.frank.locationmind.stop.servie"));
        updateService();
        setUpSchedule();
    }

    //载入菜单项
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater= getMenuInflater();
        inflater.inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
        super.onBackPressed();
    }

    //根据客户设置，服务是否运行，数据是否有更新来更新服务
    private void updateService (){
        if(!isServiceManageStopped()) {
            if (!isMyServiceRunning(MainActivity.this, com.example.frank.locationmind.GeoFenceService.class) || isServiceNeedUpdate()) {
                Intent intentToService = new Intent(MainActivity.this, GeoFenceService.class);
                intentToService.putExtra("FROM","MAINACTIVITY");
                startService(intentToService);
            }
        }
    }

    private void setUpSchedule(){
        JobInfo.Builder builder = new JobInfo.Builder(SCHEDULE_SERVICE_ID, new ComponentName(this,MySchedulerService.class));
        builder.setPeriodic(6000);
        //Android 7.0+ 增加了一项针对 JobScheduler 的新限制，最小间隔只能是下面设定的数字
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            builder.setPeriodic(JobInfo.getMinPeriodMillis(), JobInfo.getMinFlexMillis());
        builder.setPersisted(true);
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        scheduler.schedule(builder.build());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode){
            case 2501:
                Reminder rf =new Reminder(data.getDoubleExtra("LAT",0),data.getDoubleExtra("LNG",0),data.getStringExtra("TASK"));
                reminderList.add(rf);
                myAdapter.notifyDataSetChanged();
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void testReadAndWriteList(){
        /*
        ArrayList<Double> testList = new ArrayList<>();
        for (int i=0;i<10;i++) {
            testList.add(new Double(i));
        }

        String filePath = "HELLO";
        Log.i("save double","存入文件"+testList.size());

        writeListIntoFile(filePath,testList);
        testList.clear();
        Log.i("clear list","清空列表"+testList.size());
        testList = readDoubleListFromFile(filePath);
        Log.i("read list","读取列表"+testList.size());
        /*/
        Log.i("before clear","存入文件之前，reminderlist大小为"+reminderList.size());
        writeListIntoFile("HELLO",reminderList);
        reminderList.clear();
        Log.i("after clear","清空list，reminderlist大小为"+reminderList.size());
        ArrayList<Reminder> rx = readListFromFile("HELLO");
        Log.i("after read","从文件载入，reminderlist大小为"+rx.size());

        for(Reminder x:rx){
            Log.i("TASKx",x.getTaskDescription());
        }
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

    //设置是否需要重启服务更新
    private void setServieNeedUpdated(boolean needRestart){
        SharedPreferences mSharedPreferences = getSharedPreferences("REMINDER_LIST_STATES", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor= mSharedPreferences.edit();
        editor.putBoolean("SERVICE_NEED_REBOOT",needRestart);
        editor.apply();
    }

    //判断是否需要重启服务更新
    private boolean isServiceNeedUpdate(){
        SharedPreferences mSharedPreferences = getSharedPreferences("REMINDER_LIST_STATES", Context.MODE_PRIVATE);
        boolean bl = false;
        return mSharedPreferences.getBoolean("SERVICE_NEED_REBOOT",bl);
    }

    //设置用户是否停止服务
    private void setServiceManageStopped(boolean needRestart){
        SharedPreferences mSharedPreferences = getSharedPreferences("REMINDER_LIST_STATES", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor= mSharedPreferences.edit();
        editor.putBoolean("SERVICE_MANAGE_STOPPED",needRestart);
        editor.apply();
    }

    //判断用户是否需要停止服务
    private boolean isServiceManageStopped(){
        SharedPreferences mSharedPreferences = getSharedPreferences("REMINDER_LIST_STATES", Context.MODE_PRIVATE);
        boolean bl = false;
        return mSharedPreferences.getBoolean("SERVICE_MANAGE_STOPPED",bl);
    }

    //读取存储files目录下的json文件
    private  boolean initReminderListFromJSONFILE(String fileNameinFileDir){
        reminderList.clear();

        try {
            //Log.i("before open Assets","OK");
            InputStreamReader isr = new InputStreamReader(openFileInput(fileNameinFileDir),"UTF-8");
            //Log.i("after open Assets","OK");
            BufferedReader br = new BufferedReader(isr);
            String line;
            StringBuilder builder = new StringBuilder();
            while((line = br.readLine()) != null){
                builder.append(line);
            }
            br.close();
            isr.close();
            JSONObject testjson = new JSONObject(builder.toString());//builder读取了JSON中的数据。
            //直接传入JSONObject来构造一个实例
            JSONArray array = testjson.getJSONArray("REMINDERS");         //从JSONObject中取出数组对象
            for (int i = 0; i < array.length(); i++) {
                JSONObject aReminder = array.getJSONObject(i);
                String tempStr = aReminder.getString("TASK_DESC");
                //Log.i("task description",tempStr);
                reminderList.add(new Reminder(aReminder.getDouble("LAT"),aReminder.getDouble("LNG"),aReminder.getString("TASK_DESC")));//取出数组中的对象
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    //使用json文件初始化reminderList
    private boolean initDataFromJSONAsset(String jsonFileName){

        reminderList.clear();

        try {
            //Log.i("before open Assets","OK");
            InputStreamReader isr = new InputStreamReader(getApplicationContext().getAssets().open(jsonFileName),"UTF-8");
            //Log.i("after open Assets","OK");
            BufferedReader br = new BufferedReader(isr);
            String line;
            StringBuilder builder = new StringBuilder();
            while((line = br.readLine()) != null){
                builder.append(line);
            }
            br.close();
            isr.close();
            JSONObject testjson = new JSONObject(builder.toString());//builder读取了JSON中的数据。
            //直接传入JSONObject来构造一个实例
            JSONArray array = testjson.getJSONArray("REMINDERS");         //从JSONObject中取出数组对象
            for (int i = 0; i < array.length(); i++) {
                JSONObject aReminder = array.getJSONObject(i);
                String tempStr = aReminder.getString("TASK_DESC");
                //Log.i("task description",tempStr);
                reminderList.add(new Reminder(aReminder.getDouble("LAT"),aReminder.getDouble("LNG"),aReminder.getString("TASK_DESC")));//取出数组中的对象
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(mStopGeoFenceRCV);
    }

    public class MyQuickAdapter extends BaseQuickAdapter<Reminder,BaseViewHolder>{

        public MyQuickAdapter(int layoutResId, @Nullable List<Reminder> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, Reminder item) {
            helper.setText(R.id.textView,item.getTaskDescription());
            helper.setChecked(R.id.checkBox,item.isWorking());
            helper.setImageDrawable(R.id.imageView,getDrawable(R.color.colorPrimaryDark));
        }

    }

    //copy to 外部存储设备
    private static boolean copyFile(String srcFile, String destFile){
        try{
            InputStream streamFrom = new FileInputStream(srcFile);
            OutputStream streamTo = new FileOutputStream(new File(Environment.getExternalStorageDirectory(),destFile));
            byte buffer[]=new byte[1024];
            int len;
            while ((len= streamFrom.read(buffer)) > 0){
                streamTo.write(buffer, 0, len);
            }
            streamFrom.close();
            streamTo.close();
            return true;
        } catch(Exception ex){
            return false;
        }
    }

    /**
     * 复制单个文件
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     *
     * oldpath = getExternalStorageDirectory+/Download/reminders.json;
     * newpath =  getFilesDir()+/HELLO;
     */

    public void copyFileAn(String oldPath, String newPath) {
        try {
            int byteSum = 0;
            int byteRead = 0;
            File oldFile = new File(oldPath);
            if (oldFile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                int length;
                while ( (byteRead = inStream.read(buffer)) != -1) {
                    byteSum += byteRead; //字节数 文件大小
                    System.out.println(byteSum);
                    fs.write(buffer, 0, byteRead);
                }
                inStream.close();
                fs.close();
            }
        }
        catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();

        }
    }


    public boolean saveReminderListToSharePreference(ArrayList<Reminder> rl){
        SharedPreferences mSharedPreferences = getSharedPreferences("REMINDER_LIST_IN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor= mSharedPreferences.edit();
        int i=0;
        for(;i<rl.size();i++){
            editor.putString("Reminder"+Integer.toString(i),ParcelableUtil.parcelableToString(rl.get(i)));
        }
        editor.apply();
        if (i>0){
            return true;
        }else {
            return false;
        }
    }

    public void loadReminderListFromSharePreference() {
        SharedPreferences mSharedPreferences = getSharedPreferences("REMIND_LIST_IN", Context.MODE_PRIVATE);
        int i = 0;
        reminderList.clear();
        for (; i < 3; i++) {//ToDo
            String rString = "";
            mSharedPreferences.getString("Reminder" + Integer.toString(i), rString);
            reminderList.add(ParcelableUtil.stringToT(rString,Reminder.CREATOR));
        }
        Log.i("load prefer","加载share preference中的reminder，共"+reminderList.size());
    }



}
