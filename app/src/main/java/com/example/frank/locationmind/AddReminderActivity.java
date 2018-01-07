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

import com.amap.api.location.DPoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.EnumSet;

public class AddReminderActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    //控件
    private EditText editText_location;
    private ImageView imageViewAvata;

    private CheckBox checkBoxIn;
    private CheckBox checkBoxOut;
    private CheckBox checkBoxStay;

    private Button bt_back;
    private Button bt_save;

    //数据
    private Double CCPointLat, CCPointLng;
    private String TaskDescription = "";
    private String avataFileName ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);
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
                Reminder rd = createReminderFromCurrentForm();
                Log.i("SAVE", rd.toString());
                if(rd.isQualifiedReminder()) {//合格Reminder
                    //copy avatar to MapAvatar dir
                    final String From = getFilesDir() + "/" + avataFileName;//当前目录
                    final String To = getFilesDir() + "/MapAvatar/" + Integer.toString(rd.hashCode()) + ".png";
                    Log.i("SAVE ", To);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            copyFileAn(From, To);
                            Log.i("SAVE", "移动文件到MapAvatar");
                        }
                    }).start();
                    rd.thumbernailFile = To;
                    rd.working = true;
                    bd.putParcelable("REMINDER", rd);
                    it2.putExtras(bd);
                    AddReminderActivity.this.setResult(2501, it2);
                    AddReminderActivity.this.finish();
                }
            }
        });

        editText_location = (EditText)findViewById(R.id.editText_TASK);
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
                TaskDescription =s.toString();
                Log.i("Ansen","TEXTFEILD内容改变之后调用:"+s);
            }
        });

        imageViewAvata = (ImageView)findViewById(R.id.MAP_AVATA);
        imageViewAvata.setImageResource(R.color.cardview_dark_background);
        imageViewAvata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Reminder rd = createReminderFromCurrentForm();
                Bundle bd = new Bundle();
                bd.putParcelable("REMINDER",rd);
                Intent intentToAddReminderPage = new Intent(AddReminderActivity.this,MapSelectAcitivity.class);
                intentToAddReminderPage.putExtras(bd);
                intentToAddReminderPage.setAction("com.example.frank.locationmind.map.clicked");
                startActivityForResult(intentToAddReminderPage,1510);
            }
        });

        checkBoxIn = (CheckBox)findViewById(R.id.checkBox_IN);
        checkBoxIn.setOnCheckedChangeListener(this);
        checkBoxOut = (CheckBox)findViewById(R.id.checkBox_OUT);
        checkBoxOut.setOnCheckedChangeListener(this);
        checkBoxStay = (CheckBox)findViewById(R.id.checkBox_STAY);
        checkBoxStay.setOnCheckedChangeListener(this);

        Intent it = getIntent();
        setUpForm(it.getExtras());
    }

    //使用intent中的数据更新表单
    private void setUpForm(Bundle bd) {
        Reminder rd = (Reminder) bd.getParcelable("REMINDER");
        Log.i("2_SetUpForm", rd.toString());
        //currentCenterPoint.setLatitude(rd.getLat());//Dpoint 自动转化成90，180
        //currentCenterPoint.setLongitude(rd.getLng());
        CCPointLat =rd.getLat();
        CCPointLng =rd.getLng();


        TaskDescription = rd.taskDescription;
        editText_location.setText(TaskDescription);
        imageViewAvata.setImageDrawable(rd.thumbernailFile!=null?Drawable.createFromPath(rd.thumbernailFile):getDrawable(R.drawable.add_map_default));

        if (null != rd.ReminderType) {
            Log.i("S", "通过点击已存在的项目进入ADD");
            for (Reminder.LocationState s : rd.ReminderType) {
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
                }
            }
        }
    }

    private Reminder createReminderFromCurrentForm(){
        Reminder rd = new Reminder();
        rd.lat = CCPointLat;//currentCenterPoint.getLatitude();
        rd.lng = CCPointLng;//currentCenterPoint.getLongitude();
        rd.taskDescription = TaskDescription;
        Log.i("3-1_CFromForm", rd.toString());
        //需要初始化ReminderType,否则报空指针错误
        rd.ReminderType = EnumSet.noneOf(Reminder.LocationState.class);
        if (checkBoxIn.isChecked()) rd.ReminderType.add(Reminder.LocationState.GEO_IN_REMINDIE);
        if (checkBoxOut.isChecked()) rd.ReminderType.add(Reminder.LocationState.GEO_OUT_REMINDIE);
        if (checkBoxStay.isChecked()) rd.ReminderType.add(Reminder.LocationState.GEO_STAY_REMINDIE);
        Log.i("3_CFromForm", rd.toString());
        return rd;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (resultCode){
            case 2551:

                CCPointLat = data.getDoubleExtra("LAT", 0);
                CCPointLng = data.getDoubleExtra("LNG", 0);
                avataFileName = data.getStringExtra("avaFile");
                imageViewAvata.setImageDrawable(Drawable.createFromPath(getFilesDir()+"/"+ avataFileName));
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
                Log.i("COPY","复制文件"+newPath);
            }
        }
        catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();

        }
    }

}
