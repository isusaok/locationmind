package com.example.frank.locationmind

import android.app.ProgressDialog
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.*

import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.location.DPoint
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.AMap.OnCameraChangeListener
import com.amap.api.maps2d.CameraUpdate
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.CameraPosition
import com.amap.api.maps2d.model.LatLng
import com.amap.api.maps2d.model.Marker
import com.amap.api.maps2d.model.MarkerOptions
import com.amap.api.maps2d.model.MyLocationStyle
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch

import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

class MapSelectAcitivity : AppCompatActivity(), AMapLocationListener, OnCameraChangeListener, PoiSearch.OnPoiSearchListener, MapSelectedDialog.MapSelectedDialogInterface, GeocodeSearch.OnGeocodeSearchListener {

    private var mButton: Button? = null
    private var mImageView:ImageView? = null
    private var mSearchView: SearchView? = null
    private var mProgressBar:ProgressBar? =null
    private var mListView: ListView? = null
    private var mAddLocationText: TextView? = null

    //数据模型

    var centerLat: Double? = null
    var centerLng: Double? = null//临时变量，避免频繁更新currentReminder
    internal var avataFilePath = ""
    internal var LocationStr = ""
    internal var currentReminder: Reminder? = null //当前位置为基础的Reminder,仅作为Activity之间传输数据用


    internal var isInitLoad: Boolean = false

    private var mMapView: MapView? = null
    private var mAmap: AMap? = null
    private var aMapLocationClient: AMapLocationClient? = null
    private var aMapLocationClientOption: AMapLocationClientOption? = null

    //POI search
    protected var mPoiQ: PoiSearch.Query = PoiSearch.Query("","","")
    protected var mPoiS: PoiSearch? = null
    protected var cameraCityCode = "021"

    //Geocode Search
    protected var geocoderSearch: GeocodeSearch? = null

    internal var listForView = ArrayList<String>()
    internal var listOfLatLngPoint = ArrayList<LatLonPoint>()
    internal var mArrayAdapter: ArrayAdapter<String>? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isInitLoad = true
        val it = intent
        currentReminder = it.extras!!.getParcelable("REMINDER")
        Log.i("4_MapSelection", currentReminder!!.toString())


        setContentView(R.layout.activity_map_select_acitivity)
        //mButton = findViewById<View>(R.id.SEARCHVIEW_LOCATIONSELECT) as Button
        mSearchView = findViewById<View>(R.id.SEARCH_VIEW_TEXT) as SearchView
        mImageView = findViewById<View>(R.id.POINT_VIEW) as ImageView
        mProgressBar = findViewById<View>(R.id.PROGRESS_BAR) as ProgressBar
        mProgressBar!!.visibility=View.GONE
        mListView = findViewById<View>(R.id.LISTVIEW_LOCATIONSELECT) as ListView
        mAddLocationText = findViewById<View>(R.id.TEXTSELECT) as TextView
        mAddLocationText!!.setOnClickListener {
            Log.i("click add", "点击选中的地址")
            avataFilePath = "ava.png"

            Thread(Runnable {
                /**
                 * 对地图进行截屏
                 */
                mAmap!!.getMapScreenShot(object : AMap.OnMapScreenShotListener {


                    //@Override
                    override fun onMapScreenShot(bitmap: Bitmap?) {
                        Log.e("TAG", "回调1")
                        if (null == bitmap) {
                            return
                        }
                        try {

                            // ABSPath = getFilesDir()+"/"+'avataFilePath'
                            val fos = openFileOutput(avataFilePath, Context.MODE_PRIVATE)
                            Log.i("click add", "点击新建文件")
                            val b = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                            try {
                                fos.flush()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            try {
                                fos.close()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            val buffer = StringBuffer()
                            if (b)
                                buffer.append("截屏成功 ")
                            else {
                                buffer.append("截屏失败 ")
                            }
                            //ToastUtil.show(getApplicationContext(), buffer.toString());

                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        }

                    }

                    //@Override
                    fun onMapScreenShot(bitmap: Bitmap?, status: Int) {
                        Log.e("TAG", "回调2")

                        if (null == bitmap) {
                            return
                        }
                        try {
                            val fos = openFileOutput(avataFilePath, Context.MODE_PRIVATE)
                            Log.i("click add", "点击新建文件")
                            val b = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                            try {
                                fos.flush()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            try {
                                fos.close()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            val buffer = StringBuffer()
                            if (b)
                                buffer.append("截屏成功 ")
                            else {
                                buffer.append("截屏失败 ")
                            }
                            if (status != 0)
                                buffer.append("地图渲染完成，截屏无网格")
                            else {
                                buffer.append("地图未渲染完成，截屏有网格")
                            }
                            //ToastUtil.show(getApplicationContext(), buffer.toString());

                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        }

                    }
                })
            }).start()

            showMapSelectedDialog(listForView[0] + "附近")
        }


        mMapView = findViewById<View>(R.id.MAP) as MapView
        mMapView!!.onCreate(savedInstanceState)
        mAmap = mMapView!!.map

        setUpMap()
        configLocation()
        setUpGeoCodeSearch()
        //configPOISearch();

        aMapLocationClient!!.startLocation()

        mArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listForView)
        mListView!!.adapter = mArrayAdapter
        mListView!!.emptyView = findViewById<View>(R.id.EMPTYVIEW) as TextView

        mListView!!.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, id ->
            Log.i("ITEMCLICK", java.lang.Double.toString(listOfLatLngPoint[position].latitude))
            //moveCameraToLocation(new LatLng(listOfLatLngPoint.get(position).getLatitude(),listOfLatLngPoint.get(position).getLongitude()),16);
            animatedMoveCameraToLocation(LatLng(listOfLatLngPoint[position].latitude, listOfLatLngPoint[position].longitude), 16f)
        }

        mAmap!!.setOnCameraChangeListener(this)
        //设置地图点击的回调对象
        mAmap!!.setOnMapClickListener {
            //
        }

        //设置地图标记点击的回调对象
        mAmap!!.setOnMarkerClickListener {
            //
            false
        }

        //设置地图标记长按的回调对象
        mAmap!!.setOnMapLongClickListener {
            //
        }

        mAmap!!.setOnMapLoadedListener {
            Log.i("MAP LOADED", "地图加载完成")
            //Log.i("MAP LOADED",Double.toString(initCenterLat)+Double.toString(initCenterLng));
            //如果是OnCreate调用则移动到传过来的位置，否则啥也不干
            //TODO 地图切换，后续添加
            if (isInitLoad) {
                if (currentReminder!!.isQualifiedReminder)
                    moveCameraToLocation(LatLng(currentReminder!!.lat, currentReminder!!.lng), 16f)
            }
            isInitLoad = false
        }

        /*
        mButton!!.setOnClickListener { v ->
            if (v.id == R.id.SEARCHVIEW_LOCATIONSELECT) {
                onSearchRequested()
            }
        }
        */
        mSearchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                Log.i("SearchTEXT",query)
                doMySearch(query!!)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })

        handleIntent(it)

    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            Log.i("HANDLEINTENT", query)
            doMySearch(query)
        }
        if (intent.action == "com.example.frank.locationmind.map.clicked") {

        }
    }

    fun doMySearch(q: String) {
        Log.i("SEARCH FOR SEARCH", q)
        val mPoiQ: PoiSearch.Query
        val mPoiS: PoiSearch
        mPoiQ = PoiSearch.Query(q, "", cameraCityCode)
        mPoiQ.pageSize = 10
        mPoiQ.pageNum = 1

        mPoiS = PoiSearch(this@MapSelectAcitivity, mPoiQ)
        mPoiS.setOnPoiSearchListener(this@MapSelectAcitivity)
        mPoiS.searchPOIAsyn()
        mProgressBar!!.visibility = View.VISIBLE
        //Thread(Runnable {  try {Thread.sleep(3000) }catch( e:InterruptedException){e.printStackTrace()} }).start()
    }

    //对话框处理
    fun showMapSelectedDialog(msg: String) {
        val newFragment = MapSelectedDialog.newInstance(msg)
        newFragment.show(fragmentManager, "选择地图")
    }


    private fun configLocation() {
        aMapLocationClient = AMapLocationClient(this@MapSelectAcitivity)
        aMapLocationClientOption = AMapLocationClientOption()
        aMapLocationClient!!.setLocationListener(this)

        aMapLocationClientOption!!.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        //设置定位间隔,单位毫秒,默认为2000ms
        aMapLocationClientOption!!.interval = 2000
        //设置定位参数
        aMapLocationClient!!.setLocationOption(aMapLocationClientOption)
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为1000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
    }

    private fun setUpMap() {

        val myLocationStyle: MyLocationStyle
        myLocationStyle = MyLocationStyle()//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(2000) //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW)
        myLocationStyle.showMyLocation(false)
        mAmap!!.setMyLocationStyle(myLocationStyle)//设置定位蓝点的Style
        mAmap!!.uiSettings.isMyLocationButtonEnabled = true//设置默认定位按钮是否显示，非必需设置。
        mAmap!!.isMyLocationEnabled = true// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。

        //moveCameraToLocation(new LatLng(initCenterLat,initCenterLng),9);
    }

    private fun setUpGeoCodeSearch() {
        geocoderSearch = GeocodeSearch(this)
        geocoderSearch!!.setOnGeocodeSearchListener(this)
    }

    //Activity生命周期管理,在其中管理地图的生命周期
    override fun onDestroy() {
        super.onDestroy()
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView!!.onDestroy()
        //销毁定位对象
        if (null != aMapLocationClient) {
            aMapLocationClient!!.onDestroy()
        }
    }

    override fun onResume() {
        super.onResume()
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView!!.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView!!.onSaveInstanceState(outState)
    }

    //AMapLocationListener接口
    override fun onLocationChanged(aMapLocation: AMapLocation?) {
        if (aMapLocation != null) {
            if (aMapLocation.errorCode == 0) {
                //currentReminder.lat= aMapLocation.getLatitude();
                //currentReminder.lng = aMapLocation.getLongitude();
                //moveCameraToLocation(mLatlng,13);
                //mAmap.clear();
                //final Marker marker = mAmap.addMarker(new MarkerOptions().position(mLatlng).title("当前位置").snippet("DefaultMarker"))
                Log.i("onlocationchanged",
                        "success")
            } else {
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.errorCode + ", errInfo:"
                        + aMapLocation.errorInfo)
            }
        }

    }

    //move camera to Latlng,zoom to level
    private fun moveCameraToLocation(latLng: LatLng, zoomLevel: Float) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel)
        mAmap!!.moveCamera(cameraUpdate)
    }

    private fun animatedMoveCameraToLocation(latLng: LatLng, zoomLevel: Float) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel)
        mAmap!!.animateCamera(cameraUpdate)
    }

    //Poi search
    override fun onPoiSearched(poiResult: PoiResult, i: Int) {
        Log.i("PoiResult", "CITYCODE = " + cameraCityCode)
        listForView.clear()
        listOfLatLngPoint.clear()
        mArrayAdapter!!.notifyDataSetChanged()
        PoiResultToList(poiResult)
        Log.i("ONSEARCHED", poiResult.query.queryString)
        Log.i("ONSEARCHED", Integer.toString(listForView.size))
        mProgressBar!!.visibility = View.GONE
        if (listForView.size > 0) {
            LocationStr = listForView[0]
            mAddLocationText!!.text = listForView[0]
            //addMarksOnMap(listOfLatLngPoint);
            mArrayAdapter!!.notifyDataSetChanged()
        }
    }

    private fun addMarksOnMap(list: ArrayList<LatLonPoint>) {
        for (latLonPoint in list) {
            mAmap!!.addMarker(MarkerOptions().position(LatLng(latLonPoint.latitude, latLonPoint.longitude)).title("S"))
            //final Marker marker = mAmap.addMarker(new MarkerOptions().position(new LatLng(centerLat,centerLng)).title("当前地点").snippet("DefaultMarker"));
        }
    }

    private fun PoiResultToList(poiResult: PoiResult) {
        val mList = poiResult.pois

        if (mList.size > 0) {
            for (poi in mList) {
                listForView.add(poi.title)
                listOfLatLngPoint.add(poi.latLonPoint)
            }
        } else {
            Log.i("PoiSearch Result", "error,no result")
        }
    }


    override fun onPoiItemSearched(poiItem: PoiItem, i: Int) {

    }

    override fun onCameraChange(cameraPosition: CameraPosition) {
        mAmap!!.clear()
    }

    override fun onCameraChangeFinish(cameraPosition: CameraPosition) {

        // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系

        Log.i("CameraChangeFinish", "可见视图改变完成")
        centerLat = cameraPosition.target.latitude
        centerLng = cameraPosition.target.longitude

        val query = RegeocodeQuery(LatLonPoint(centerLat!!, centerLng!!), 200f, GeocodeSearch.AMAP)
        geocoderSearch!!.getFromLocationAsyn(query)

        mAmap!!.addMarker(MarkerOptions().position(LatLng(centerLat!!, centerLng!!)).title("当前地点").snippet("DefaultMarker"))
        listForView.clear()
        listOfLatLngPoint.clear()
        configCameraPOISearch()
        mPoiS!!.bound = PoiSearch.SearchBound(LatLonPoint(centerLat!!, centerLng!!), 1000)
        mPoiS!!.searchPOIAsyn()
        mProgressBar!!.visibility = View.VISIBLE
    }

    private fun configCameraPOISearch() {
        mPoiQ = PoiSearch.Query("", "", cameraCityCode)
        mPoiQ.pageSize = 10
        mPoiQ.pageNum = 1

        mPoiS = PoiSearch(this@MapSelectAcitivity, mPoiQ)
        mPoiS!!.setOnPoiSearchListener(this@MapSelectAcitivity)
    }

    //Dialog callback
    override fun onMapSelected(isConfirmed: Boolean) {
        if (isConfirmed) {
            val it = Intent()
            val bd = Bundle()
            Log.i("5-1_MapSelection", currentReminder!!.toString())
            currentReminder!!.lat = centerLat!!
            currentReminder!!.lng = centerLng!!
            currentReminder!!.placeDescription = LocationStr + "附近"
            currentReminder!!.thumbernailFile = filesDir.toString() + "/" + avataFilePath
            Log.i("5_MapSelection", currentReminder!!.toString())
            bd.putParcelable("REMINDER", currentReminder)
            it.putExtras(bd)
            this@MapSelectAcitivity.setResult(2551, it)
            this@MapSelectAcitivity.finish()
        }
    }

    //latlng to address
    override fun onRegeocodeSearched(regeocodeResult: RegeocodeResult, i: Int) {
        if (i == 1000) {
            Log.i("CameraChangeFinish", "逆地理编码成功")
            cameraCityCode = regeocodeResult.regeocodeAddress.cityCode
        }
    }

    //address to Latlng
    override fun onGeocodeSearched(geocodeResult: GeocodeResult, i: Int) {

    }
}
