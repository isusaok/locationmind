package com.example.frank.locationmind

import android.app.ActivityManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton

import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.chad.library.adapter.base.listener.OnItemSwipeListener

import org.json.JSONArray
import org.json.JSONObject

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.util.ArrayList

import java.lang.System.*

class MainActivity : CheckPermissionsActivity() {
    private var reminderList: ArrayList<Reminder>? = ArrayList()
    private var scheduler: JobScheduler? = null

    private var mRecyclerView: RecyclerView? = null
    private var fabAdd: FloatingActionButton? = null
    private var toolbar: Toolbar? = null

    private var localBroadcastManager: LocalBroadcastManager? = null
    private var mStopGeoFenceRCV: StopGeoFenceReceiver? = null

    internal var myAdapter: MyQuickAdapter?=null
    private var LLM: LinearLayoutManager? = null

    private val view: View?
        get() = null

    //判断是否需要重启服务更新
    private val isServiceNeedUpdate: Boolean
        get() {
            val mSharedPreferences = getSharedPreferences("REMINDER_LIST_STATES", Context.MODE_PRIVATE)
            val bl = false
            return mSharedPreferences.getBoolean("SERVICE_NEED_REBOOT", bl)
        }

    //判断用户是否需要停止服务
    //设置用户是否停止服务
    private var isServiceManageStopped: Boolean
        get() {
            val mSharedPreferences = getSharedPreferences("REMINDER_LIST_STATES", Context.MODE_PRIVATE)
            val bl = false
            return mSharedPreferences.getBoolean("SERVICE_MANAGE_STOPPED", bl)
        }
        set(needRestart) {
            val mSharedPreferences = getSharedPreferences("REMINDER_LIST_STATES", Context.MODE_PRIVATE)
            val editor = mSharedPreferences.edit()
            editor.putBoolean("SERVICE_MANAGE_STOPPED", needRestart)
            editor.apply()
        }

    fun getReminderList(): List<Reminder>? {
        if (reminderList == null)
            reminderList = ArrayList()
        return reminderList
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        isServiceManageStopped = false
        val MapViewDir = File(filesDir.toString() + "/MapAvatar")
        MapViewDir.mkdir()//自建目录

        //JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);

        mRecyclerView = findViewById<View>(R.id.RCV_MAIN) as RecyclerView

        //floating Action button setup
        fabAdd = findViewById<View>(R.id.FAB_MAIN) as FloatingActionButton

        fabAdd!!.setOnClickListener {
            //增加提醒按钮
            val rd = Reminder()
            rd.lng = 1080.0
            rd.lat = rd.lng//设置默认的围栏中心点，不合格值，要求用户必须更改，否则不能保存
            rd.diameter = 500.0//设置默认的围栏直径
            val bd = Bundle()
            Log.i("1_fabADD", rd.toString())
            bd.putParcelable("REMINDER", rd)
            val intentToAddReminderPage = Intent(this@MainActivity, AddReminderActivity::class.java)
            intentToAddReminderPage.putExtras(bd)
            intentToAddReminderPage.action = "com.example.frank.locationmind.add.new"
            startActivityForResult(intentToAddReminderPage, 1500)
        }

        //toolbar setup
        toolbar = findViewById<View>(R.id.toolbar_V) as Toolbar
        setSupportActionBar(toolbar)


        toolbar!!.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.STOP_MENU -> {
                    //点击关闭服务
                    if (!isServiceManageStopped) {
                        Log.i("STOP MENU", "停止服务")
                        if (isMyServiceRunning(this@MainActivity, com.example.frank.locationmind.GeoFenceService::class.java)) {
                            val intent = Intent("com.example.frank.locationmind.stop.servie")
                            localBroadcastManager!!.sendBroadcast(intent)
                        }
                        if (null != scheduler) {
                            Log.i("STOP MENU", "scheduler 非空")
                            scheduler!!.cancelAll()
                        }
                        isServiceManageStopped = true
                    } else {//点击停止服务
                        Log.i("STOP MENU", "开启服务")
                        startService()
                        setUpAndStartSchedule()//设置并启动Scheduler
                        isServiceManageStopped = false//
                    }
                    invalidateOptionsMenu()//设置菜单需要重绘，会调用OnPrepareOptionMenu
                }
                R.id.UPDATE_MENU -> writeListToFileOnThread(dataFileURI, reminderList)
                R.id.OTHER_MENU -> if (isMyServiceRunning(this@MainActivity, com.example.frank.locationmind.GeoFenceService::class.java)) {
                    val intent = Intent("com.example.frank.locationmind.stop.servie")
                    localBroadcastManager!!.sendBroadcast(intent)
                }
                else -> {
                }
            }
            true
        }


        //load reminders from file
        loadRemindersFromFile(dataFileURI)

        //设置适配器
        myAdapter = MyQuickAdapter(R.layout.cardlayout, reminderList)
        //myAdapter.addHeaderView(getView());
        LLM = LinearLayoutManager(this)
        mRecyclerView!!.layoutManager = LLM
        mRecyclerView!!.adapter = myAdapter
        mRecyclerView!!.addItemDecoration(SimplePaddingDecoration(this))

        //滑动删除与拖曳排序
        val itemDragAndSwipeCallback = ItemDragAndSwipeCallback(myAdapter)
        val itemTouchHelper = ItemTouchHelper(itemDragAndSwipeCallback)
        itemTouchHelper.attachToRecyclerView(mRecyclerView)

        val onItemDragListener = object : OnItemDragListener {
            override fun onItemDragStart(viewHolder: RecyclerView.ViewHolder, pos: Int) {}
            override fun onItemDragMoving(source: RecyclerView.ViewHolder, from: Int, target: RecyclerView.ViewHolder, to: Int) {}
            override fun onItemDragEnd(viewHolder: RecyclerView.ViewHolder, pos: Int) {}
        }

        val onItemSwipeListener = object : OnItemSwipeListener {
            override fun onItemSwipeStart(viewHolder: RecyclerView.ViewHolder, pos: Int) {}
            override fun clearView(viewHolder: RecyclerView.ViewHolder, pos: Int) {}
            override fun onItemSwiped(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                val AFP = reminderList!![pos].thumbernailFile
                if (null != AFP) {
                    val file = File(AFP)
                    file.delete()
                    if (file.exists()) Log.i("DELETE", "删除文件失败")
                }
            }

            override fun onItemSwipeMoving(canvas: Canvas, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, isCurrentlyActive: Boolean) {

            }
        }

        // open drag
        myAdapter!!.enableDragItem(itemTouchHelper, R.id.imageView, true)
        myAdapter!!.setOnItemDragListener(onItemDragListener)

        // open slide to delete
        myAdapter!!.enableSwipeItem()
        myAdapter!!.setOnItemSwipeListener(onItemSwipeListener)
        //滑动删除与拖曳排序

        myAdapter!!.setOnItemClickListener(object : BaseQuickAdapter.OnItemClickListener {
            override fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
                Log.d("item click", "onItemClick: ")
                val rd = reminderList!![position]
                val bd = Bundle()
                bd.putParcelable("REMINDER", rd)
                val it = Intent(this@MainActivity, AddReminderActivity::class.java)
                it.action = "com.example.frank.locationmind.update.reminder"
                it.putExtras(bd)
                startActivityForResult(it, 1590)
            }
        })


        myAdapter?.setOnItemChildClickListener { adapter, view, position ->
            Log.d("item child click", "onItemChildClick: ")
            val rd = reminderList!![position]
            val bd = Bundle()
            bd.putParcelable("REMINDER", rd)
            val it = Intent(this@MainActivity, AddReminderActivity::class.java)
            it.action = "com.example.frank.locationmind.update.reminder"
            it.putExtras(bd)
            startActivityForResult(it, 1590)
        }


        val it = intent
        if (it.action == "com.example.frank.location.notify.start") {
            LLM!!.scrollToPosition(it.getIntExtra("NOTIFIED_ITEM", 0))
        }

        //writeListIntoFile("HELLO",reminderList);*/
        setServiceNeedUpdated(true)

        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        mStopGeoFenceRCV = StopGeoFenceReceiver()
        localBroadcastManager!!.registerReceiver(mStopGeoFenceRCV, IntentFilter("com.example.frank.locationmind.stop.servie"))
        updateService()

        if (!isServiceManageStopped)
            setUpAndStartSchedule()
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == "com.example.frank.location.notify.start")
            LLM!!.scrollToPosition(intent.getIntExtra("NOTIFIED_ITEM", 0))
    }

    //载入菜单项
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val update_item_text = if (isServiceManageStopped) "开启服务" else "关闭服务"
        menu.findItem(R.id.STOP_MENU).title = update_item_text
        menu.findItem(R.id.OTHER_MENU).title = if (isMyServiceRunning(this@MainActivity, com.example.frank.locationmind.GeoFenceService::class.java)) "服务运行中" else "服务已停止"
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onBackPressed() {
        moveTaskToBack(false)
        super.onBackPressed()
    }

    private fun loadRemindersFromFile(fileName: String) {
        val ABSFilePath = filesDir.toString() + "/" + fileName
        val file = File(ABSFilePath)
        if (file.exists()) {//确认文件存在，否则报错？
            reminderList = readListFromFile(dataFileURI)
            Log.i("after read", reminderList!!.toString())
        }
    }

    //根据客户设置，服务是否运行，数据是否有更新来更新服务
    private fun updateService() {
        if (!isMyServiceRunning(this@MainActivity, com.example.frank.locationmind.GeoFenceService::class.java) || isServiceNeedUpdate) {
            val intentToService = Intent(this@MainActivity, GeoFenceService::class.java)
            intentToService.putExtra("FROM", "MAINACTIVITY")
            startService(intentToService)
        }
    }

    private fun startService() {
        val intentToService = Intent(this@MainActivity, GeoFenceService::class.java)
        intentToService.putExtra("FROM", "MAINACTIVITY")
        startService(intentToService)
    }

    private fun setUpAndStartSchedule() {
        val builder = JobInfo.Builder(SCHEDULE_SERVICE_ID, ComponentName(this, MySchedulerService::class.java))
        builder.setPeriodic(6000)
        //Android 7.0+ 增加了一项针对 JobScheduler 的新限制，最小间隔只能是下面设定的数字
        scheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            builder.setPeriodic(JobInfo.getMinPeriodMillis(), JobInfo.getMinFlexMillis())
        builder.setPersisted(true)
        scheduler!!.schedule(builder.build())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (resultCode) {
            2501 -> {
                val rf = data.extras!!.getParcelable("REMINDER") as Reminder
                reminderList!!.add(rf)
                myAdapter?.notifyDataSetChanged()
                writeListToFileOnThread(dataFileURI, reminderList)
                val intentToService = Intent(this@MainActivity, GeoFenceService::class.java)
                intentToService.putExtra("FROM", "NEW PLACE")
                startService(intentToService)
            }
        }
        for (rd in reminderList!!) {
            Log.i("SAVED REMINDER", rd.toString())
        }
        super.onActivityResult(requestCode, resultCode, data)

    }

    //对象数组存入文件
    fun writeListIntoFile(fileName: String, stus: ArrayList<*>?) {
        val oos: ObjectOutputStream
        try {
            val fos = openFileOutput(fileName, Context.MODE_PRIVATE)
            oos = ObjectOutputStream(BufferedOutputStream(fos))
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

    private fun writeListToFileOnThread(fileName: String, list: ArrayList<*>?) {
        Thread(Runnable { writeListIntoFile(fileName, list) }).start()
        Log.i("Write List", "在线程中更新列表到文件")
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


    //设置是否需要重启服务更新
    private fun setServiceNeedUpdated(needRestart: Boolean) {
        val mSharedPreferences = getSharedPreferences("REMINDER_LIST_STATES", Context.MODE_PRIVATE)
        val editor = mSharedPreferences.edit()
        editor.putBoolean("SERVICE_NEED_REBOOT", needRestart)
        editor.apply()
    }

    //读取存储files目录下的json文件
    private fun initReminderListFromJSONFILE(fileNameinFileDir: String): Boolean {
        reminderList!!.clear()

        try {
            //Log.i("before open Assets","OK");
            val isr = InputStreamReader(openFileInput(fileNameinFileDir), "UTF-8")
            //Log.i("after open Assets","OK");
            val br = BufferedReader(isr)
            var line: String?=null
            val builder = StringBuilder()
            while (line != null) {
                builder.append(line)
                line = br.readLine()
            }
            br.close()
            isr.close()
            val testjson = JSONObject(builder.toString())//builder读取了JSON中的数据。
            //直接传入JSONObject来构造一个实例
            val array = testjson.getJSONArray("REMINDERS")         //从JSONObject中取出数组对象
            for (i in 0 until array.length()) {
                val aReminder = array.getJSONObject(i)
                val tempStr = aReminder.getString("TASK_DESC")
                //Log.i("task description",tempStr);
                reminderList!!.add(Reminder(aReminder.getDouble("LAT"), aReminder.getDouble("LNG"), aReminder.getString("TASK_DESC")))//取出数组中的对象
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }


    //使用json文件初始化reminderList
    private fun initDataFromJSONAsset(jsonFileName: String): Boolean {

        reminderList!!.clear()

        try {
            //Log.i("before open Assets","OK");
            val isr = InputStreamReader(applicationContext.assets.open(jsonFileName), "UTF-8")
            //Log.i("after open Assets","OK");
            val br = BufferedReader(isr)
            var line: String?=null
            val builder = StringBuilder()
            while (line != null) {
                builder.append(line)
                line = br.readLine()
            }
            br.close()
            isr.close()
            val testjson = JSONObject(builder.toString())//builder读取了JSON中的数据。
            //直接传入JSONObject来构造一个实例
            val array = testjson.getJSONArray("REMINDERS")         //从JSONObject中取出数组对象
            for (i in 0 until array.length()) {
                val aReminder = array.getJSONObject(i)
                val tempStr = aReminder.getString("TASK_DESC")
                //Log.i("task description",tempStr);
                reminderList!!.add(Reminder(aReminder.getDouble("LAT"), aReminder.getDouble("LNG"), aReminder.getString("TASK_DESC")))//取出数组中的对象
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager!!.unregisterReceiver(mStopGeoFenceRCV)
    }

    override fun onStop() {
        super.onStop()
        writeListIntoFile(dataFileURI, reminderList)//Onstop的执行可以得到保证，Ondestroy的执行不能保证
    }

    inner class MyQuickAdapter(layoutResId: Int, data: List<Reminder>?) : BaseItemDraggableAdapter<Reminder, BaseViewHolder>(layoutResId, data) {

        override fun convert(helper: BaseViewHolder, item: Reminder) {
            helper.setText(R.id.textView, "在" + item.placeDescription!!.trim { it <= ' ' } + item.taskDescription)
            helper.setChecked(R.id.checkBox, item.isWorking)
            helper.setImageDrawable(R.id.imageView, Drawable.createFromPath(item.thumbernailFile))
            helper.addOnClickListener(R.id.imageView)
            helper.setOnCheckedChangeListener(R.id.checkBox) { buttonView, isChecked ->
                item.isWorking = isChecked
                writeListToFileOnThread(dataFileURI, reminderList)
                val presentCheck = buttonView as CheckBox
                val checked = presentCheck.isChecked
                val a = if (checked) "正在运行" else "已停止"
                presentCheck.text = a
            }

        }
    }


    fun copyFileAn(oldPath: String, newPath: String) {
        try {
            var byteSum = 0
            var byteRead = 0
            val oldFile = File(oldPath)
            if (oldFile.exists()) { //文件存在时
                val inStream = FileInputStream(oldPath) //读入原文件
                val fs = FileOutputStream(newPath)
                val buffer = ByteArray(1444)
                val length: Int
                while (byteRead != -1) {
                    byteSum += byteRead //字节数 文件大小
                    println(byteSum)
                    fs.write(buffer, 0, byteRead)
                    byteRead = inStream.read(buffer)
                }
                inStream.close()
                fs.close()
            }
        } catch (e: Exception) {
            println("复制单个文件操作出错")
            e.printStackTrace()

        }

    }


    fun saveReminderListToSharePreference(rl: ArrayList<Reminder>): Boolean {
        val mSharedPreferences = getSharedPreferences("REMINDER_LIST_IN", Context.MODE_PRIVATE)
        val editor = mSharedPreferences.edit()
        var i = 0
        while (i < rl.size) {
            editor.putString("Reminder" + Integer.toString(i), ParcelableUtil.parcelableToString(rl[i]))
            i++
        }
        editor.apply()
        return if (i > 0) {
            true
        } else {
            false
        }
    }

    fun loadReminderListFromSharePreference() {
        val mSharedPreferences = getSharedPreferences("REMIND_LIST_IN", Context.MODE_PRIVATE)
        var i = 0
        reminderList!!.clear()
        while (i < 3) {//ToDo
            val rString = ""
            mSharedPreferences.getString("Reminder" + Integer.toString(i), rString)
            reminderList!!.add(ParcelableUtil.stringToT(rString, Reminder.CREATOR))
            i++
        }
        Log.i("load prefer", "加载share preference中的reminder，共" + reminderList!!.size)
    }

    companion object {

        private val dataFileURI = "HELLO"
        private val SCHEDULE_SERVICE_ID = 9001

        //copy to 外部存储设备
        private fun copyFile(srcFile: String, destFile: String): Boolean {
            try {
                val streamFrom = FileInputStream(srcFile)
                val streamTo = FileOutputStream(File(Environment.getExternalStorageDirectory(), destFile))
                val buffer = ByteArray(1024)
                var len: Int= 0
                while (len > 0) {
                    streamTo.write(buffer, 0, len)
                    len = streamFrom.read(buffer)
                }
                streamFrom.close()
                streamTo.close()
                return true
            } catch (ex: Exception) {
                return false
            }

        }
    }

}
