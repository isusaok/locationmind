package com.example.frank.locationmind;


/**
 * Created by mingxingsong on 2017/12/10.
 */

import android.support.v7.widget.RecyclerView;
import android.content.Context;
import android.view.View;
import android.graphics.Rect;


public class SimplePaddingDecoration extends RecyclerView.ItemDecoration {
    private int dividerHeight;


    public SimplePaddingDecoration(Context context) {
        dividerHeight = context.getResources().getDimensionPixelSize(R.dimen.divider_height);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.top= dividerHeight;
        outRect.bottom = dividerHeight;//类似加了一个bottom padding
    }

}
