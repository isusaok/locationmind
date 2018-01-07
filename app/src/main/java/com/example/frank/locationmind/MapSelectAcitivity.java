package com.example.frank.locationmind;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.DPoint;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMap.OnCameraChangeListener;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MapSelectAcitivity extends AppCompatActivity
                                implements AMapLocationListener,
                                            OnCameraChangeListener,
                                            PoiSearch.OnPoiSearchListener,
                                            MapSelectedDialog.MapSelectedDialogInterface {

    private Button mButton;
    private SearchView mSearchView;
    private ListView mListView;
    private TextView mAddLocationText;

    //数据模型
    Double centerLat,centerLng;
    String avataFilePath ="";
    Reminder initReminder = new Reminder(); //当前位置为基础的Reminder


    boolean isInitLoad;

    public Double getCenterLat() {
        return centerLat;
    }

    public void setCenterLat(Double centerLat) {
        this.centerLat = centerLat;
    }

    public Double getCenterLng() {
        return centerLng;
    }

    public void setCenterLng(Double centerLng) {
        this.centerLng = centerLng;
    }

    private MapView mMapView;
    private AMap mAmap;
    private AMapLocationClient aMapLocationClient;
    private AMapLocationClientOption aMapLocationClientOption;

    //POI search
    protected PoiSearch.Query mPoiQ;
    protected PoiSearch mPoiS;

    ArrayList<String> listForView = new ArrayList<String>();
    ArrayList<LatLonPoint> listOfLatLngPoint = new ArrayList<LatLonPoint>();
    ArrayAdapter<String> mArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isInitLoad = true;
        Intent it = getIntent();
        initReminder = (Reminder) it.getExtras().getParcelable("REMINDER");
        Log.i("4_MapSelection", initReminder.toString());


        setContentView(R.layout.activity_map_select_acitivity);
        mButton = (Button)findViewById(R.id.SEARCHVIEW_LOCATIONSELECT);
        mSearchView = (SearchView)findViewById(R.id.SEARCH_VIEW_TEXT);
        mListView =(ListView)findViewById(R.id.LISTVIEW_LOCATIONSELECT);
        mAddLocationText = (TextView)findViewById(R.id.TEXTSELECT);
        mAddLocationText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i("click add","点击选中的地址");
                avataFilePath = "ava.png";

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        /**
                         * 对地图进行截屏
                         */
                        mAmap.getMapScreenShot(new AMap.OnMapScreenShotListener() {


                            //@Override
                            public void onMapScreenShot(Bitmap bitmap) {
                                Log.e("TAG", "回调1");
                                if(null == bitmap){
                                    return;
                                }
                                try {
                                    FileOutputStream fos = openFileOutput(avataFilePath, Context.MODE_PRIVATE);
                                    Log.i("click add","点击新建文件");
                                    boolean b = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                    try {
                                        fos.flush();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        fos.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    StringBuffer buffer = new StringBuffer();
                                    if (b)
                                        buffer.append("截屏成功 ");
                                    else {
                                        buffer.append("截屏失败 ");
                                    }
                                    //ToastUtil.show(getApplicationContext(), buffer.toString());

                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }

                            }

                            //@Override
                            public void onMapScreenShot(Bitmap bitmap, int status) {
                                Log.e("TAG", "回调2");

                                if(null == bitmap){
                                    return;
                                }
                                try {
                                    FileOutputStream fos = openFileOutput(avataFilePath, Context.MODE_PRIVATE);
                                    Log.i("click add","点击新建文件");
                                    boolean b = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                    try {
                                        fos.flush();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        fos.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    StringBuffer buffer = new StringBuffer();
                                    if (b)
                                        buffer.append("截屏成功 ");
                                    else {
                                        buffer.append("截屏失败 ");
                                    }
                                    if (status != 0)
                                        buffer.append("地图渲染完成，截屏无网格");
                                    else {
                                        buffer.append( "地图未渲染完成，截屏有网格");
                                    }
                                    //ToastUtil.show(getApplicationContext(), buffer.toString());

                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }

                            }
                        });

                    }
                }).start();

                showMapSelectedDialog(listForView.get(0)+"附近");

            }
        });




        mMapView = (MapView)findViewById(R.id.MAP);
        mMapView.onCreate(savedInstanceState);
        mAmap = mMapView.getMap();

        setUpMap();
        configLocation();
        configPOISearch();

        aMapLocationClient.startLocation();

        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listForView);
        mListView.setAdapter(mArrayAdapter);
        mListView.setEmptyView((TextView)findViewById(R.id.EMPTYVIEW));

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("ITEMCLICK",Double.toString(listOfLatLngPoint.get(position).getLatitude()));
                moveCameraToLocation(new LatLng(listOfLatLngPoint.get(position).getLatitude(),listOfLatLngPoint.get(position).getLongitude()),16);
            }
        });


        mAmap.setOnCameraChangeListener(this);
        //设置地图点击的回调对象
        mAmap.setOnMapClickListener(new AMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //
            }
        });

        //设置地图标记点击的回调对象
        mAmap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //
                return false;
            }
        });

        //设置地图标记长按的回调对象
        mAmap.setOnMapLongClickListener(new AMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                //
            }
        });

        mAmap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
            @Override
            public void onMapLoaded() {
                Log.i("MAP LOADED","地图加载完成");
                //Log.i("MAP LOADED",Double.toString(initCenterLat)+Double.toString(initCenterLng));
                if(isInitLoad){
                    if(initReminder.isQualifiedReminder())
                        moveCameraToLocation(new LatLng(initReminder.getLat(),initReminder.getLng()),16);
                }
                isInitLoad = false;
            }
        });

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.getId()==R.id.SEARCHVIEW_LOCATIONSELECT){
                    onSearchRequested();
                }
            }
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        handleIntent(it);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.i("HANDLEINTENT",query);
            doMySearch(query);
        }
    }

    public void doMySearch(String q){
        Log.i("DOSEARCH",q);
        PoiSearch.Query mPoiQ;
        PoiSearch mPoiS;
        mPoiQ = new PoiSearch.Query(q,"","021");
        mPoiQ.setPageSize(10);
        mPoiQ.setPageNum(1);

        mPoiS = new PoiSearch(MapSelectAcitivity.this,mPoiQ);
        mPoiS.setOnPoiSearchListener(MapSelectAcitivity.this);
        mPoiS.searchPOIAsyn();
    }

    //对话框处理
    public void showMapSelectedDialog(String msg) {
        MapSelectedDialog newFragment = MapSelectedDialog.newInstance(msg);
        newFragment.show(getFragmentManager(), "选择地图");
    }


    private void configPOISearch(){
        mPoiQ = new PoiSearch.Query("","","021");
        mPoiQ.setPageSize(10);
        mPoiQ.setPageNum(1);

        mPoiS = new PoiSearch(MapSelectAcitivity.this,mPoiQ);
        mPoiS.setOnPoiSearchListener(MapSelectAcitivity.this);
    }

    private  void configLocation(){
        aMapLocationClient = new AMapLocationClient(MapSelectAcitivity.this);
        aMapLocationClientOption = new AMapLocationClientOption();
        aMapLocationClient.setLocationListener(this);

        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms
        aMapLocationClientOption.setInterval(2000);
        //设置定位参数
        aMapLocationClient.setLocationOption(aMapLocationClientOption);
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为1000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
    }

    private void setUpMap(){

        MyLocationStyle myLocationStyle;
        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW);
        myLocationStyle.showMyLocation(false);
        mAmap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        mAmap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
        mAmap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。

        //moveCameraToLocation(new LatLng(initCenterLat,initCenterLng),9);
    }

    //Activity生命周期管理,在其中管理地图的生命周期
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
        //销毁定位对象
        if(null != aMapLocationClient){
            aMapLocationClient.onDestroy();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    //AMapLocationListener接口
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if(aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0){
                //initReminder.lat= aMapLocation.getLatitude();
                //initReminder.lng = aMapLocation.getLongitude();
                //moveCameraToLocation(mLatlng,13);
                //mAmap.clear();
                //final Marker marker = mAmap.addMarker(new MarkerOptions().position(mLatlng).title("当前位置").snippet("DefaultMarker"))
                Log.i("onlocationchanged",
                        "success");
            } else {
                Log.e("AmapError","location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }

    }

    private void moveCameraToLocation (LatLng latLng, float zoomLevel) {
        CameraUpdateFactory cameraUpdateF = new CameraUpdateFactory();
        CameraUpdate cameraUpdate = cameraUpdateF.newLatLngZoom(latLng, zoomLevel);
        mAmap.moveCamera(cameraUpdate);
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
        listForView.clear();
        listOfLatLngPoint.clear();
        PoiResultToList(poiResult);
        Log.i("ONSEARCHED",Integer.toString(listForView.size()));
        Log.i("ONSEARCHED",poiResult.getQuery().getQueryString());
        mAddLocationText.setText(listForView.get(0));
        mArrayAdapter.notifyDataSetChanged();
    }

    private void PoiResultToList(PoiResult poiResult){
        ArrayList<PoiItem> mList = poiResult.getPois();

        if(mList.size()>0){
            for (PoiItem poi :mList){
                listForView.add(poi.getTitle());
                listOfLatLngPoint.add(poi.getLatLonPoint());
            }
        }else{
            Log.i("PoiSearch Result","error,no result");
        }

    }


    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        mAmap.clear();

    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {


        Log.i("CameraChangeFinish", "run");
        centerLat = cameraPosition.target.latitude;
        centerLng = cameraPosition.target.longitude;
        final Marker marker = mAmap.addMarker(new MarkerOptions().position(new LatLng(centerLat,centerLng)).title("当前地点").snippet("DefaultMarker"));
        listForView.clear();
        listOfLatLngPoint.clear();
        mPoiS.setBound(new PoiSearch.SearchBound(new LatLonPoint(centerLat,centerLng),1000));
        mPoiS.searchPOIAsyn();
    }

    @Override
    public void onMapSelected(boolean isConfirmed) {
        if (isConfirmed){
            Intent it = new Intent();
            it.putExtra("LAT",centerLat);
            it.putExtra("LNG",centerLng);
            it.putExtra("avaFile",avataFilePath);
            MapSelectAcitivity.this.setResult(2551,it);
            MapSelectAcitivity.this.finish();
        }
    }
}
