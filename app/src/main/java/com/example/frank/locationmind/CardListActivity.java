package com.example.frank.locationmind;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class CardListActivity extends AppCompatActivity {

    private String strReminderFile = "f.con";
    private @Nullable List<Reminder> reminderList = new ArrayList<>();
    private FloatingActionButton fab01Add;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_list);

        fab01Add = (FloatingActionButton)findViewById(R.id.floatingActionButton);
        fab01Add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //增加提醒按钮
                Intent intentToAddReminderPage = new Intent(CardListActivity.this, AddReminderActivity.class);
                startActivityForResult(intentToAddReminderPage,1);
            }
        });

        initData();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycleCard);

        //设置recycleView的adapter
        MyAdapter adapter = new MyAdapter(this, reminderList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));//为recyclerview的layout设立一个layoutmanager
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new SimplePaddingDecoration(this));

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode){
            case 100:
                Log.i("save","OK");
                break;
            case 101:
                Log.i("cancel","Fail");
                break;
            default:
                Log.i("wrong" ,
                        "no back info");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveAllRemindersToFile(strReminderFile);
    }

    private void initData() {
        //reminderList =null;
        //readAllRemindersFromFile(strReminderFile);
        Reminder r1 = new Reminder(43,122,"wk1dsdddsddsdsdsd");
        Reminder r2 = new Reminder(43,122,"wk1fsssssssssssss");
        Reminder r3 = new Reminder(43,122,"wk1ggggggggggggg");

        reminderList.add(r1);
        reminderList.add(r2);
        reminderList.add(r3);

        /*
        reminderList = new ArrayList<>();
        int[] imgs = new int[]{R.drawable.p1,R.drawable.p2,R.drawable.p3,R.drawable.p4,R.drawable.p5,R.drawable.p6,
                R.drawable.p7,R.drawable.p8,R.drawable.p9,R.drawable.p10,R.drawable.p11,R.drawable.p12,R.drawable.p13,
                R.drawable.p14,R.drawable.p15};
        for (int img:imgs) {
            ItemEntity itemEntity = new ItemEntity();
            itemEntity.setContent("风光  " + img);
            itemEntity.setImg(img);
            list.add(itemEntity);
        }
    */

    }


    private void readAllRemindersFromFile(String fileName) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = openFileInput(fileName);
            ois = new ObjectInputStream(fis);
            reminderList = (List<Reminder>) ois.readObject();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                ois.close();
                fis.close();
            }catch (Exception e){
        }

        }
    }

    private void saveAllRemindersToFile (String fileName)  {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = openFileOutput(fileName, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(reminderList);
            oos.flush();
        } catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                oos.close();
                fos.close();
            }catch (Exception e){

            }
        }
    }


    public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {


        private List<Reminder> list;
        private Context context;
        private LayoutInflater inflater;

        public MyAdapter(Context context, List<Reminder> list) {
            this.context = context;
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        //step2:使用inflater展开定制view的布局文件cardlayout.xml
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = inflater.inflate(R.layout.cardlayout,parent,false);
            return new MyViewHolder(view);
        }

        //step3:绑定viewHolder的数据
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            MyViewHolder holder1 = (MyViewHolder) holder;//实例化占位符
            Reminder itemReminder = list.get(position);//根据位置索引获取数据
            holder1.content.setText(itemReminder.getTaskDescription());//设置占位符中各成员变量的实际内容
            holder1.imageView.setImageResource(R.drawable.a2017121839);//设置占位符中各成员变量的实际内容
            holder1.checkBox.setChecked(itemReminder.isWorking());
        }

        //返回整个链表的长度，是指Adapter要构建的所有视图的总和，包括表头，表尾等
        @Override
        public int getItemCount() {
            return list.size();
        }

        

        //step1:定义viewHolder的布局结构，viewhodle相当于占位符，在viewholder的构造函数中，要将定制View布局文件中的视觉元素对应到Viewholder的成员变量中
        class MyViewHolder extends RecyclerView.ViewHolder{

            private ImageView imageView;
            private TextView content;
            private CheckBox checkBox;
            public MyViewHolder(View itemView) {
                super(itemView);
                imageView = (ImageView) itemView.findViewById(R.id.imageView);
                content = (TextView) itemView.findViewById(R.id.textView);
                checkBox = (CheckBox) itemView.findViewById(R.id.checkBox);

            }
        }
    }
}
