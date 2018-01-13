package com.example.frank.locationmind


/**
 * Created by mingxingsong on 2017/12/10.
 */

import android.support.v7.widget.RecyclerView
import android.content.Context
import android.view.View
import android.graphics.Rect


class SimplePaddingDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val dividerHeight: Int


    init {
        dividerHeight = context.resources.getDimensionPixelSize(R.dimen.divider_height)
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.top = dividerHeight
        outRect.bottom = dividerHeight
        outRect.left = dividerHeight * 2
        outRect.right = dividerHeight * 2
    }

}
