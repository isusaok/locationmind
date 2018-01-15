package com.example.frank.locationmind

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView

import com.amap.api.location.DPoint

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.util.ArrayList
import java.util.EnumSet

import java.lang.System.out

class AddReminderActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {

    //控件
    private var editText_location: EditText? = null
    private var editText_Task: EditText? = null
    private var imageViewAvata: ImageView? = null
    private var seekBar: SeekBar? = null
    private var textView: TextView? = null

    private var checkBoxIn: CheckBox? = null
    private var checkBoxOut: CheckBox? = null
    private var checkBoxStay: CheckBox? = null

    private var bt_back: Button? = null
    private var bt_save: Button? = null

    //数据
    private val CCPointLat: Double? = null
    private val CCPointLng: Double? = null
    private val TaskDescription = ""
    private val avataFileName = "ava.png"
    private val LocationDescription = ""
    private val geoFenceDiameter: Double = 0.toDouble()
    private var currentReminder: Reminder? = null


    private var reminderList: MutableList<Reminder>? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_reminder)

        imageViewAvata = findViewById<View>(R.id.MAP_AVATA) as ImageView
        imageViewAvata!!.setOnClickListener {
            val bd = Bundle()
            bd.putParcelable("REMINDER", currentReminder)
            val intentToAddReminderPage = Intent(this@AddReminderActivity, MapSelectAcitivity::class.java)
            intentToAddReminderPage.putExtras(bd)
            intentToAddReminderPage.action = "com.example.frank.locationmind.map.clicked"
            startActivityForResult(intentToAddReminderPage, 1510)
        }

        editText_location = findViewById<View>(R.id.editText_PLACE) as EditText
        editText_location!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                Log.i("Ansen", "TEXTFIELD内容改变之前调用:" + s)
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                Log.i("Ansen", "TEXTFIELD内容改变，可以去告诉服务器:" + s)
            }

            override fun afterTextChanged(s: Editable) {
                currentReminder!!.placeDescription = s.toString()
                Log.i("Ansen", "TEXTFEILD内容改变之后调用:" + s)
            }
        })

        editText_Task = findViewById<View>(R.id.editText_TASK) as EditText
        editText_Task!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                currentReminder!!.taskDescription = s.toString()
            }
        })

        textView = findViewById<View>(R.id.textView_DIAMETER) as TextView

        seekBar = findViewById<View>(R.id.seekBar_DIAMETER) as SeekBar
        seekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentReminder!!.diameter = (500 + 2500 * progress / 100).toDouble()
                textView!!.text = diameterDescriptionFromMeter(currentReminder!!.diameter)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {


            }
        })



        checkBoxIn = findViewById<View>(R.id.checkBox_IN) as CheckBox
        checkBoxIn!!.setOnCheckedChangeListener(this)
        checkBoxOut = findViewById<View>(R.id.checkBox_OUT) as CheckBox
        checkBoxOut!!.setOnCheckedChangeListener(this)
        checkBoxStay = findViewById<View>(R.id.checkBox_STAY) as CheckBox
        checkBoxStay!!.setOnCheckedChangeListener(this)

        bt_back = findViewById<View>(R.id.button_CANCEL) as Button
        bt_back!!.setOnClickListener {
            val it1 = Intent()
            it1.putExtra("some", 12)
            this@AddReminderActivity.setResult(2500, it1)
            this@AddReminderActivity.finish()
        }

        bt_save = findViewById<View>(R.id.button_SAVE) as Button
        bt_save!!.setOnClickListener {
            val it2 = Intent()
            val bd = Bundle()
            //Log.i("BEFORE SAVE LAST ", reminderList.get(reminderList.size()-1).toString());
            Log.i("SAVE AFTER CREATE", currentReminder!!.toString())
            Log.i("ALL reminders", reminderList!!.toString())
            if (currentReminder!!.isQualifiedReminder && !isInReminderList(currentReminder, reminderList)) {//合格Reminder
                //copy avatar to MapAvatar dir From ABSPath,but don't know h
                val From = currentReminder!!.thumbernailFile//当前图片,ABSPath
                val To = filesDir.toString() + "/MapAvatar/" + Integer.toString(currentReminder!!.hashCodeExThumber()) + ".png"
                Thread(Runnable { copyFileAn(From, To) }).start()
                currentReminder!!.thumbernailFile = To
                currentReminder!!.isWorking = true
                bd.putParcelable("REMINDER", currentReminder!!)
                it2.putExtras(bd)
                this@AddReminderActivity.setResult(2501, it2)
                this@AddReminderActivity.finish()
            }
        }


        val it = intent
        setUpFormA(it.extras)
        loadRemindersFromFile(dataFileURI)
    }

    private fun diameterDescriptionFromMeter(meter:Double):String{
        var diameterStr:String
        when {
            meter< 1000 -> diameterStr = String.format("%d米",(meter/10).toInt()*10)
            else -> diameterStr = String.format("%.1f公里",meter/1000)
        }
        return diameterStr
    }

    private fun isInReminderList(rd: Reminder?, list: MutableList<Reminder>?): Boolean {
        for (trd in list!!) {
            if (rd == trd) return true
        }
        return false
    }

    private fun setUpFormA(bd: Bundle?) {

        //TODO setup data
        currentReminder = bd!!.getParcelable("REMINDER")
        Log.i("2+1 SETUPFORM", currentReminder!!.toString())

        //setup form

        editText_Task!!.setText(if (currentReminder!!.taskDescription != null) currentReminder!!.taskDescription else "")
        editText_location!!.setText(if (currentReminder!!.placeDescription != null) currentReminder!!.placeDescription else "")

        textView!!.text = diameterDescriptionFromMeter(if (currentReminder!!.diameter < 500) 500.toDouble() else currentReminder!!.diameter)

        val progress = ((currentReminder!!.diameter - 500) / 2500 * 100).toInt()
        seekBar!!.progress = progress
        Log.i("2+2 SETUPFORM", currentReminder!!.toString())

        imageViewAvata!!.setImageDrawable(if (currentReminder!!.thumbernailFile != null) Drawable.createFromPath(currentReminder!!.thumbernailFile) else getDrawable(R.drawable.add_map_default))

        //注意，改变check的状态会变更
        if (null != currentReminder!!.ReminderType) {
            val ls = currentReminder!!.ReminderType!!.clone()
            for (s in currentReminder!!.ReminderType!!) {
                when (s) {
                    Reminder.LocationState.GEO_IN_REMINDIE -> checkBoxIn!!.isChecked = true
                    Reminder.LocationState.GEO_OUT_REMINDIE -> checkBoxOut!!.isChecked = true
                    Reminder.LocationState.GEO_STAY_REMINDIE -> checkBoxStay!!.isChecked = true
                    else -> {
                    }
                }
            }
            currentReminder!!.ReminderType = ls
        }
    }


    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (null == currentReminder!!.ReminderType)
            currentReminder!!.ReminderType = EnumSet.noneOf(Reminder.LocationState::class.java)

        when (buttonView.id) {
            R.id.checkBox_IN -> if (isChecked) {
                currentReminder!!.ReminderType!!.add(Reminder.LocationState.GEO_IN_REMINDIE)
            } else {
                currentReminder!!.ReminderType!!.remove(Reminder.LocationState.GEO_IN_REMINDIE)
            }
            R.id.checkBox_OUT -> if (isChecked) {
                currentReminder!!.ReminderType!!.add(Reminder.LocationState.GEO_OUT_REMINDIE)
            } else {
                currentReminder!!.ReminderType!!.remove(Reminder.LocationState.GEO_OUT_REMINDIE)
            }
            R.id.checkBox_STAY -> if (isChecked) {
                currentReminder!!.ReminderType!!.add(Reminder.LocationState.GEO_STAY_REMINDIE)
            } else {
                currentReminder!!.ReminderType!!.remove(Reminder.LocationState.GEO_STAY_REMINDIE)
            }
            else -> {
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (resultCode) {
            2551 -> setUpFormA(data!!.extras)
        }
        if (null != data) {

        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    fun copyFileAn(oldPath: String?, newPath: String) {
        try {
            var byteSum = 0
            var byteRead = 0
            val oldFile = File(oldPath!!)
            if (oldFile.exists()) { //文件存在时
                val inStream = FileInputStream(oldPath) //读入原文件
                val fs = FileOutputStream(newPath)//如果目录不存在要自己建目录
                val buffer = ByteArray(1444)
                while (byteRead != -1) {
                    byteSum += byteRead //字节数 文件大小
                    println(byteSum)
                    fs.write(buffer, 0, byteRead)
                    byteRead = inStream.read(buffer)
                }
                inStream.close()
                fs.close()
                Log.i("COPY", "从" + oldPath + "复制到" + newPath)
            }
        } catch (e: Exception) {
            println("复制单个文件操作出错")
            e.printStackTrace()
        }

    }

    private fun loadRemindersFromFile(fileName: String) {
        val ABSFilePath = filesDir.toString() + "/" + fileName
        val file = File(ABSFilePath)
        if (file.exists()) {//确认文件存在，否则报错？
            reminderList = readListFromFile(dataFileURI)
            Log.i("load all reminders", reminderList!!.toString())
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

    companion object {

        private val dataFileURI = "HELLO"
    }

}
