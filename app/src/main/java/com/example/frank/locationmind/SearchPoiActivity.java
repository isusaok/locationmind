package com.example.frank.locationmind;

import android.app.Activity;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import java.util.ArrayList;

/**
 * Created by frank on 17/12/17.
 */

public class SearchPoiActivity extends Activity
                                implements PoiSearch.OnPoiSearchListener{

    private ArrayAdapter<String> mArrayAdapter;
    private ArrayList<String> mListForResult;

    private ListView mListView;
    private TextView mtextView;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_poi);
        mListView =(ListView)findViewById(R.id.list);
        mtextView = (TextView)findViewById(R.id.empty);

        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,mListForResult);
        handleIntent(getIntent());
        }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doMySearch(query);
        }
    }

    public void doMySearch(String q){
        PoiSearch.Query mPoiQ;
        PoiSearch mPoiS;
        mPoiQ = new PoiSearch.Query("","","021");
        mPoiQ.setPageSize(10);
        mPoiQ.setPageNum(1);

        mPoiS = new PoiSearch(SearchPoiActivity.this,mPoiQ);
        mPoiS.setOnPoiSearchListener(SearchPoiActivity.this);
        mPoiS.searchPOIAsyn();

    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
        mListForResult.clear();
        PoiResultToList(poiResult);
        mArrayAdapter.notifyDataSetChanged();

    }

    private void PoiResultToList(PoiResult poiResult){
        ArrayList<PoiItem> mList = poiResult.getPois();

        if(mList.size()>0){
            for (PoiItem poi :mList){
                mListForResult.add(poi.getTitle());
            }
        }else{
            Log.i("PoiSearch Result","error,no result");
        }

    }


    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }
}
