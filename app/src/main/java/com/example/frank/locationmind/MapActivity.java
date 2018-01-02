package com.example.frank.locationmind;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import android.widget.PopupWindow;


import java.util.Date;

public class MapActivity extends AppCompatActivity
                            implements AMap.OnMapLongClickListener,
                                        AMap.OnMarkerClickListener,
                                        AMapLocationListener,
                                        PoiSearch.OnPoiSearchListener {
    private double lat;
    private double lot;

    protected MapView mapView = null;
    protected  AMap aMap = null;
    protected PopupWindow popWindow;

    //声明mlocationClient对象
    public AMapLocationClient mlocationClient;
    //声明mLocationOption对象
    public AMapLocationClientOption mLocationOption = null;

    //POI search

    protected PoiSearch.Query mPoiQ;
    protected PoiSearch mPoiS;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mapView = (MapView) findViewById(R.id.MAP);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        aMap = mapView.getMap();

        aMap.setOnMapLongClickListener(this);
        aMap.setOnMarkerClickListener(this);

        mlocationClient = new AMapLocationClient(this);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位监听
        mlocationClient.setLocationListener(this);
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        //设置定位参数
        mlocationClient.setLocationOption(mLocationOption);
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为1000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        //启动定位
        mlocationClient.startLocation();

        //POI search
        mPoiQ = new PoiSearch.Query("","050300","021");
        mPoiQ.setPageSize(10);
        mPoiQ.setPageNum(1);

        mPoiS = new PoiSearch(this,mPoiQ);
        mPoiS.setOnPoiSearchListener(this);

    }

    private void initMap(){

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mapView.onDestroy()，销毁地图
        mapView.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mapView.onResume ()，重新绘制加载地图
        mapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mapView.onPause ()，暂停地图的绘制
        mapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        aMap.clear();
        final Marker marker = aMap.addMarker(new MarkerOptions().position(latLng).title("北京").snippet("DefaultMarker"));
        mPoiS.setBound(new PoiSearch.SearchBound(new LatLonPoint(marker.getPosition().latitude,
                marker.getPosition().longitude), 10000));
        mPoiS.searchPOIAsyn();
        showPopupWindowFromBottom();


    }

    public void showPopupWindowFromBottom() {
        View view = MapActivity.this.getLayoutInflater().inflate(R.layout.popuplayout,null);

        popWindow = new PopupWindow(view,300,200);
        popWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F8F8F8")));
        // TODO: 2016/5/17 设置可以获取焦点
        popWindow.setFocusable(true);
        // TODO: 2016/5/17 设置可以触摸弹出框以外的区域
        popWindow.setOutsideTouchable(true);
        // TODO：更新popupwindow的状态
        popWindow.update();

        popWindow.showAtLocation(MapActivity.this.findViewById(R.id.toolbar), Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,10,10);

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        //从marker中获取坐标，启用一个新的activity
        return false;
        //true 返回true表示该点击事件已被处理，不再往下传递（如底图点击不会被触发），返回false则继续往下传递。
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                aMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                double mlat = aMapLocation.getLatitude();//获取纬度
                double mlog = aMapLocation.getLongitude();//获取经度
                aMapLocation.getAccuracy();//获取精度信息
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(aMapLocation.getTime());
                df.format(date);//定位时间
                String locstr = aMapLocation.toString();
                Log.i("shanghai","location loaded");
                Log.i("AMapLocation", locstr);
                changeLocation(new LatLng(mlat,mlog));

            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError","location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }

    public void changeLocation(LatLng latLng) {
        CameraUpdateFactory cameraUpdateF = new CameraUpdateFactory();
        CameraUpdate cameraUpdate = cameraUpdateF.newLatLngZoom(latLng, 19);
        aMap.moveCamera(cameraUpdate);
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
        //
        int co = poiResult.getPageCount();
        String sCount = co +"";
        String sInfo = i+"";
        Log.i("some count some count",sCount);
        Log.i("error code",sInfo);
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }
}
