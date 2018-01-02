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

public class AddReminderActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {


    private EditText editText_location;
    private String TaskDescription = "";
    private ImageView imageViewAvata;

    private CheckBox checkBoxIn;
    private CheckBox checkBoxOut;
    private CheckBox checkBoxStay;

    private Button bt_back ;
    private Button bt_save;

    private DPoint currentCenterPoint = new DPoint();

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
                it2.putExtra("another",132);
                it2.putExtra("LAT",currentCenterPoint.getLatitude());
                it2.putExtra("LNG",currentCenterPoint.getLongitude());
                it2.putExtra("TASK",TaskDescription);
                AddReminderActivity.this.setResult(2501,it2);
                AddReminderActivity.this.finish();
            }
        });

        editText_location = (EditText)findViewById(R.id.editText_TASK);
        editText_location.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.i("Ansen","内容改变之前调用:"+s);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.i("Ansen","内容改变，可以去告诉服务器:"+s);
            }

            @Override
            public void afterTextChanged(Editable s) {
                TaskDescription =s.toString();
                Log.i("Ansen","内容改变之后调用:"+s);
            }
        });

        imageViewAvata = (ImageView)findViewById(R.id.MAP_AVATA);
        imageViewAvata.setImageResource(R.color.cardview_dark_background);
        imageViewAvata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(AddReminderActivity.this,MapSelectAcitivity.class);
                it.putExtra("Source","110");
                startActivityForResult(it,1510);
            }
        });

        checkBoxIn = (CheckBox)findViewById(R.id.checkBox_IN);
        checkBoxIn.setOnCheckedChangeListener(this);
        checkBoxOut = (CheckBox)findViewById(R.id.checkBox_OUT);
        checkBoxOut.setOnCheckedChangeListener(this);
        checkBoxStay = (CheckBox)findViewById(R.id.checkBox_STAY);
        checkBoxStay.setOnCheckedChangeListener(this);

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (resultCode){
            case 2551:
                currentCenterPoint.setLatitude(data.getDoubleExtra("LAT_CENTER", 0));
                currentCenterPoint.setLongitude(data.getDoubleExtra("LNG_CENTER", 0));
                imageViewAvata.setImageDrawable(Drawable.createFromPath(getFilesDir()+"/"+ data.getStringExtra("avaFile")));
        }
        if (null!=data) {


        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
