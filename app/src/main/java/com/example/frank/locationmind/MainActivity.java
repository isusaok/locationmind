package com.example.frank.locationmind;

import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback;
import com.chad.library.adapter.base.listener.OnItemDragListener;
import com.chad.library.adapter.base.listener.OnItemSwipeListener;

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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.*;

public class MainActivity extends CheckPermissionsActivity {

    private static final String dataFileURI = "HELLO";
    private static final int SCHEDULE_SERVICE_ID = 9001;
    private @Nullable ArrayList<Reminder> reminderList = new ArrayList<Reminder>();
    private JobScheduler scheduler;

    private RecyclerView mRecyclerView;
    private FloatingActionButton fabAdd;
    private Toolbar toolbar;

    private LocalBroadcastManager localBroadcastManager;
    private StopGeoFenceReceiver mStopGeoFenceRCV;

    MyQuickAdapter myAdapter;
    private  LinearLayoutManager LLM;

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
        File MapViewDir = new File(getFilesDir()+"/MapAvatar");
        MapViewDir.mkdir();//自建目录

        //JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);

        mRecyclerView = (RecyclerView)findViewById(R.id.RCV_MAIN);

        //floating Action button setup
        fabAdd = (FloatingActionButton)findViewById(R.id.FAB_MAIN);

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //增加提醒按钮
                Reminder rd = new Reminder();
                rd.lat=rd.lng=1080;//设置默认的围栏中心点，不合格值，要求用户必须更改，否则不能保存
                rd.diameter = 500F;//设置默认的围栏直径
                Bundle bd = new Bundle();
                Log.i("1_fabADD",rd.toString());
                bd.putParcelable("REMINDER",rd);
                Intent intentToAddReminderPage = new Intent(MainActivity.this, AddReminderActivity.class);
                intentToAddReminderPage.putExtras(bd);
                intentToAddReminderPage.setAction("com.example.frank.locationmind.add.new");
                startActivityForResult(intentToAddReminderPage,1500);
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
                                                           //点击关闭服务
                                                           if(!isServiceManageStopped()){
                                                               Log.i("STOP MENU","停止服务");
                                                               if(isMyServiceRunning(MainActivity.this, com.example.frank.locationmind.GeoFenceService.class)){
                                                                   Intent intent = new Intent("com.example.frank.locationmind.stop.servie");
                                                                   localBroadcastManager.sendBroadcast(intent);
                                                               }
                                                               if(null!=scheduler){
                                                                   Log.i("STOP MENU","scheduler 非空");
                                                                   scheduler.cancelAll();
                                                               }
                                                               setServiceManageStopped(true);
                                                           }else {//点击停止服务
                                                               Log.i("STOP MENU","开启服务");
                                                               startService();
                                                               setUpAndStartSchedule();//设置并启动Scheduler
                                                               setServiceManageStopped(false);//
                                                           }
                                                           invalidateOptionsMenu();//设置菜单需要重绘，会调用OnPrepareOptionMenu
                                                           break;
                                                       case R.id.UPDATE_MENU:
                                                           writeListToFileOnThread(dataFileURI,reminderList);
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


        //load reminders from file
        loadRemindersFromFile(dataFileURI);

        //设置适配器
        myAdapter = new MyQuickAdapter(R.layout.cardlayout,reminderList);
        //myAdapter.addHeaderView(getView());
        LLM = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(LLM);
        mRecyclerView.setAdapter(myAdapter);
        mRecyclerView.addItemDecoration(new SimplePaddingDecoration(this));

        //滑动删除与拖曳排序
        ItemDragAndSwipeCallback itemDragAndSwipeCallback = new ItemDragAndSwipeCallback(myAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemDragAndSwipeCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        OnItemDragListener onItemDragListener = new OnItemDragListener() {
            @Override
            public void onItemDragStart(RecyclerView.ViewHolder viewHolder, int pos){}
            @Override
            public void onItemDragMoving(RecyclerView.ViewHolder source, int from, RecyclerView.ViewHolder target, int to) {}
            @Override
            public void onItemDragEnd(RecyclerView.ViewHolder viewHolder, int pos) {}
        };

        OnItemSwipeListener onItemSwipeListener = new OnItemSwipeListener() {
            @Override
            public void onItemSwipeStart(RecyclerView.ViewHolder viewHolder, int pos) {}
            @Override
            public void clearView(RecyclerView.ViewHolder viewHolder, int pos) {}
            @Override
            public void onItemSwiped(RecyclerView.ViewHolder viewHolder, int pos) {
                String AFP = reminderList.get(pos).getThumbernailFile();
                if (null!=AFP){
                    File  file = new File(AFP);
                    file.delete();
                    if(file.exists()) Log.i("DELETE","删除文件失败");
                }
            }

            @Override
            public void onItemSwipeMoving(Canvas canvas, RecyclerView.ViewHolder viewHolder, float dX, float dY, boolean isCurrentlyActive) {

            }
        };

        // open drag
        myAdapter.enableDragItem(itemTouchHelper, R.id.imageView, true);
        myAdapter.setOnItemDragListener(onItemDragListener);

        // open slide to delete
        myAdapter.enableSwipeItem();
        myAdapter.setOnItemSwipeListener(onItemSwipeListener);
        //滑动删除与拖曳排序

        myAdapter.setOnItemClickListener(new MyQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                Log.d("item click", "onItemClick: ");
                Reminder rd = reminderList.get(position);
                Bundle bd = new Bundle();
                bd.putParcelable("REMINDER",rd);
                Intent it = new Intent(MainActivity.this,AddReminderActivity.class);
                it.setAction("com.example.frank.locationmind.update.reminder");
                it.putExtras(bd);
                startActivityForResult(it,1590);
            }
        });


        myAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                Log.d("item child click", "onItemChildClick: ");
                Reminder rd = reminderList.get(position);
                Bundle bd = new Bundle();
                bd.putParcelable("REMINDER",rd);
                Intent it = new Intent(MainActivity.this,AddReminderActivity.class);
                it.setAction("com.example.frank.locationmind.update.reminder");
                it.putExtras(bd);
                startActivityForResult(it,1590);
            }
        });


        Intent it = getIntent();
        if (it.getAction().equals("com.example.frank.location.notify.start")){
            LLM.scrollToPosition(it.getIntExtra("NOTIFIED_ITEM",0));}

        //writeListIntoFile("HELLO",reminderList);*/
        setServiceNeedUpdated(true);

        localBroadcastManager= LocalBroadcastManager.getInstance(this);

        mStopGeoFenceRCV = new StopGeoFenceReceiver();
        localBroadcastManager.registerReceiver(mStopGeoFenceRCV,new IntentFilter("com.example.frank.locationmind.stop.servie"));
        updateService();

        if(!isServiceManageStopped())
            setUpAndStartSchedule();
    }

    private View getView() {
        return null;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getAction().equals("com.example.frank.location.notify.start"))
            LLM.scrollToPosition(intent.getIntExtra("NOTIFIED_ITEM",0));
    }

    //载入菜单项
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater= getMenuInflater();
        inflater.inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String update_item_text = isServiceManageStopped()?"开启服务":"关闭服务";
        menu.findItem(R.id.STOP_MENU).setTitle(update_item_text);
        menu.findItem(R.id.OTHER_MENU).setTitle(isMyServiceRunning(MainActivity.this, com.example.frank.locationmind.GeoFenceService.class)?"服务运行中":"服务已停止");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
        super.onBackPressed();
    }

    private void loadRemindersFromFile(String fileName){
        String ABSFilePath = getFilesDir()+"/"+fileName;
        File file = new File(ABSFilePath);
        if (file.exists()){//确认文件存在，否则报错？
            reminderList = readListFromFile(dataFileURI);
            Log.i("after read",reminderList.toString());
        }
    }
    //根据客户设置，服务是否运行，数据是否有更新来更新服务
    private void updateService (){
            if (!isMyServiceRunning(MainActivity.this, com.example.frank.locationmind.GeoFenceService.class) || isServiceNeedUpdate()) {
                Intent intentToService = new Intent(MainActivity.this, GeoFenceService.class);
                intentToService.putExtra("FROM","MAINACTIVITY");
                startService(intentToService);
            }
    }

    private void startService(){
        Intent intentToService = new Intent(MainActivity.this, GeoFenceService.class);
        intentToService.putExtra("FROM","MAINACTIVITY");
        startService(intentToService);
    }

    private void setUpAndStartSchedule(){
        JobInfo.Builder builder = new JobInfo.Builder(SCHEDULE_SERVICE_ID, new ComponentName(this,MySchedulerService.class));
        builder.setPeriodic(6000);
        //Android 7.0+ 增加了一项针对 JobScheduler 的新限制，最小间隔只能是下面设定的数字
        scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            builder.setPeriodic(JobInfo.getMinPeriodMillis(), JobInfo.getMinFlexMillis());
        builder.setPersisted(true);
        scheduler.schedule(builder.build());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode){
            case 2501:
                Reminder rf = (Reminder)data.getExtras().getParcelable("REMINDER");
                reminderList.add(rf);
                myAdapter.notifyDataSetChanged();
                writeListToFileOnThread(dataFileURI,reminderList);
                Intent intentToService = new Intent(MainActivity.this, GeoFenceService.class);
                intentToService.putExtra("FROM","NEW PLACE");
                startService(intentToService);
        }
        for(Reminder rd:reminderList){
            Log.i("SAVED REMINDER",rd.toString());
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    //对象数组存入文件
    public void writeListIntoFile(String fileName,ArrayList stus){
        ObjectOutputStream oos;
        try {
            FileOutputStream fos = openFileOutput(fileName,Context.MODE_PRIVATE);
            oos = new ObjectOutputStream( new BufferedOutputStream(fos));
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

    private void writeListToFileOnThread(final String fileName, final ArrayList list){
        new Thread(new Runnable() {
            @Override
            public void run() {
                writeListIntoFile(fileName,list);
            }
        }).start();
        Log.i("Write List","在线程中更新列表到文件");
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
    private void setServiceNeedUpdated(boolean needRestart){
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

    @Override
    protected void onStop() {
        super.onStop();
        writeListIntoFile(dataFileURI,reminderList);//Onstop的执行可以得到保证，Ondestroy的执行不能保证
    }

    public class MyQuickAdapter extends BaseItemDraggableAdapter<Reminder,BaseViewHolder> {

        public MyQuickAdapter(int layoutResId, @Nullable List<Reminder> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, final Reminder item) {
            helper.setText(R.id.textView,"在"+item.getPlaceDescription().trim()+item.getTaskDescription());
            helper.setChecked(R.id.checkBox,item.isWorking());
            helper.setImageDrawable(R.id.imageView,Drawable.createFromPath(item.thumbernailFile));
            helper.addOnClickListener(R.id.imageView);
            helper.setOnCheckedChangeListener(R.id.checkBox, new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    item.working = isChecked;
                    writeListToFileOnThread(dataFileURI,reminderList);
                    CheckBox presentCheck = (CheckBox)(buttonView);
                    boolean checked =presentCheck.isChecked();
                    String a = checked?"正在运行":"已停止";
                    presentCheck.setText(a);
                }
            });

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
