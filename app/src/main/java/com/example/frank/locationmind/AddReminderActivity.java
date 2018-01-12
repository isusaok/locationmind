package com.example.frank.locationmind;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.amap.api.location.DPoint;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.EnumSet;

import static java.lang.System.out;

public class AddReminderActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    //控件
    private EditText editText_location;
    private EditText editText_Task;
    private ImageView imageViewAvata;
    private SeekBar seekBar;
    private TextView textView;

    private CheckBox checkBoxIn;
    private CheckBox checkBoxOut;
    private CheckBox checkBoxStay;

    private Button bt_back;
    private Button bt_save;

    //数据
    private Double CCPointLat, CCPointLng;
    private String TaskDescription = "";
    private String avataFileName ="ava.png";
    private String LocationDescription ="";
    private double geoFenceDiameter;
    private Reminder currentReminder;


    private ArrayList<Reminder> reminderList;

    private static final String dataFileURI = "HELLO";

    public Reminder getCurrentReminder() {
        if (currentReminder==null)
            currentReminder = new Reminder();
        return currentReminder;
    }

    public void setCurrentReminder(Reminder currentReminder) {
        this.currentReminder = currentReminder;
    }

    public ArrayList<Reminder> getReminderList() {
        if (reminderList==null)
            reminderList = new ArrayList<Reminder>();
        return reminderList;
    }

    public void setReminderList(ArrayList<Reminder> reminderList) {
        this.reminderList = reminderList;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        imageViewAvata = (ImageView)findViewById(R.id.MAP_AVATA);
        imageViewAvata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bd = new Bundle();
                bd.putParcelable("REMINDER",currentReminder);
                Intent intentToAddReminderPage = new Intent(AddReminderActivity.this,MapSelectAcitivity.class);
                intentToAddReminderPage.putExtras(bd);
                intentToAddReminderPage.setAction("com.example.frank.locationmind.map.clicked");
                startActivityForResult(intentToAddReminderPage,1510);
            }
        });

        editText_location = (EditText)findViewById(R.id.editText_PLACE);
        editText_location.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.i("Ansen","TEXTFIELD内容改变之前调用:"+s);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.i("Ansen","TEXTFIELD内容改变，可以去告诉服务器:"+s);
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentReminder.placeDescription =s.toString();
                Log.i("Ansen","TEXTFEILD内容改变之后调用:"+s);
            }
        });

        editText_Task =(EditText)findViewById(R.id.editText_TASK);
        editText_Task.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                currentReminder.taskDescription =s.toString();
            }
        });

        textView = (TextView)findViewById(R.id.textView_DIAMETER);

        seekBar =(SeekBar)findViewById(R.id.seekBar_DIAMETER);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentReminder.diameter= 500+(2500*(progress)/100);
                textView.setText(Double.toString(currentReminder.diameter)+"米");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


            }
        });



        checkBoxIn = (CheckBox)findViewById(R.id.checkBox_IN);
        checkBoxIn.setOnCheckedChangeListener(this);
        checkBoxOut = (CheckBox)findViewById(R.id.checkBox_OUT);
        checkBoxOut.setOnCheckedChangeListener(this);
        checkBoxStay = (CheckBox)findViewById(R.id.checkBox_STAY);
        checkBoxStay.setOnCheckedChangeListener(this);

        bt_back = (Button) findViewById(R.id.button_CANCEL);
        bt_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it1 = new Intent();
                it1.putExtra("some",12);
                AddReminderActivity.this.setResult(2500,it1);
                AddReminderActivity.this.finish();
            }
        });

        bt_save = (Button)findViewById(R.id.button_SAVE);
        bt_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it2 = new Intent();
                Bundle bd = new Bundle();
                //Log.i("BEFORE SAVE LAST ", reminderList.get(reminderList.size()-1).toString());
                Log.i("SAVE AFTER CREATE", currentReminder.toString());
                Log.i("ALL reminders",reminderList.toString());
                if(currentReminder.isQualifiedReminder()&&(!isInReminderList(currentReminder,reminderList))) {//合格Reminder
                    //copy avatar to MapAvatar dir From ABSPath,but don't know h
                    final String From = currentReminder.thumbernailFile;//当前图片,ABSPath
                    final String To = getFilesDir() + "/MapAvatar/" + Integer.toString(currentReminder.hashCodeExThumber()) + ".png";
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            copyFileAn(From, To);
                        }
                    }).start();
                    currentReminder.thumbernailFile = To;
                    currentReminder.working = true;
                    bd.putParcelable("REMINDER", currentReminder);
                    it2.putExtras(bd);
                    AddReminderActivity.this.setResult(2501, it2);
                    AddReminderActivity.this.finish();
                }
            }
        });




        Intent it = getIntent();
        setUpFormA(it.getExtras());
        loadRemindersFromFile(dataFileURI);
    }

    private boolean isInReminderList(Reminder rd, ArrayList<Reminder> list){
        for(Reminder trd:list){
            if (rd.equals(trd)) return true;
        }
        return false;
    }

    private void setUpFormA(Bundle bd){

        //TODO setup data
        currentReminder = (Reminder) bd.getParcelable("REMINDER");
        Log.i("2+1 SETUPFORM", currentReminder.toString());

        //setup form

        editText_Task.setText(currentReminder.taskDescription!=null?currentReminder.taskDescription:"");
        editText_location.setText(currentReminder.placeDescription!=null?currentReminder.placeDescription:"");

        textView.setText(Double.toString(currentReminder.diameter<500?500F:currentReminder.diameter)+"米");

        int progress =(int)(((currentReminder.diameter-500)/2500)*100);
        seekBar.setProgress(progress);
        Log.i("2+2 SETUPFORM", currentReminder.toString());

        imageViewAvata.setImageDrawable(currentReminder.thumbernailFile!=null?Drawable.createFromPath(currentReminder.thumbernailFile):getDrawable(R.drawable.add_map_default));

        //注意，改变check的状态会变更
        if (null != currentReminder.ReminderType) {
            EnumSet<Reminder.LocationState> ls = currentReminder.ReminderType.clone();
            for (Reminder.LocationState s : currentReminder.ReminderType) {
                switch (s) {
                    case GEO_IN_REMINDIE:
                        checkBoxIn.setChecked(true);
                        break;
                    case GEO_OUT_REMINDIE:
                        checkBoxOut.setChecked(true);
                        break;
                    case GEO_STAY_REMINDIE:
                        checkBoxStay.setChecked(true);
                        break;
                    default:
                        break;
                }
            }
            currentReminder.ReminderType = ls;
        }
    }



    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(null==currentReminder.ReminderType)
            currentReminder.ReminderType = EnumSet.noneOf(Reminder.LocationState.class);

        switch (buttonView.getId()){
            case R.id.checkBox_IN:
                if (isChecked) {
                    currentReminder.ReminderType.add(Reminder.LocationState.GEO_IN_REMINDIE);
                }else {
                    currentReminder.ReminderType.remove(Reminder.LocationState.GEO_IN_REMINDIE);
                }
                break;
            case R.id.checkBox_OUT:
                if (isChecked) {
                    currentReminder.ReminderType.add(Reminder.LocationState.GEO_OUT_REMINDIE);
                }else {
                    currentReminder.ReminderType.remove(Reminder.LocationState.GEO_OUT_REMINDIE);
                }
                break;
            case R.id.checkBox_STAY:
                if (isChecked) {
                    currentReminder.ReminderType.add(Reminder.LocationState.GEO_STAY_REMINDIE);
                }else {
                    currentReminder.ReminderType.remove(Reminder.LocationState.GEO_STAY_REMINDIE);
                }
                break;
            default:
                break;

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (resultCode){
            case 2551:
                setUpFormA(data.getExtras());

        }
        if (null!=data) {

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void copyFileAn(String oldPath, String newPath) {
        try {
            int byteSum = 0;
            int byteRead = 0;
            File oldFile = new File(oldPath);
            if (oldFile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);//如果目录不存在要自己建目录
                byte[] buffer = new byte[1444];
                int length;
                while ( (byteRead = inStream.read(buffer)) != -1) {
                    byteSum += byteRead; //字节数 文件大小
                    System.out.println(byteSum);
                    fs.write(buffer, 0, byteRead);
                }
                inStream.close();
                fs.close();
                Log.i("COPY","从"+oldPath+"复制到"+newPath);
            }
        }
        catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();
        }
    }

    private void loadRemindersFromFile(String fileName){
        String ABSFilePath = getFilesDir()+"/"+fileName;
        File file = new File(ABSFilePath);
        if (file.exists()){//确认文件存在，否则报错？
            reminderList = readListFromFile(dataFileURI);
            Log.i("load all reminders",reminderList.toString());
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

}
