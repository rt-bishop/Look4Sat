package com.rtbishop.look4sat.utility

import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class RecyclerDivider(private val resId: Int) : RecyclerView.ItemDecoration() {

    override fun onDrawOver(c: Canvas, parent: RecyclerView) {
        val divider = ContextCompat.getDrawable(parent.context, resId)!!
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        parent.adapter?.itemCount?.let { childCount ->
            for (i in 0 until childCount) {
                if (i == (childCount - 1)) continue
                parent.getChildAt(i)?.let {
                    val params = it.layoutParams as RecyclerView.LayoutParams
                    val top = it.bottom + params.bottomMargin
                    val bottom = top + divider.intrinsicHeight
                    divider.setBounds(left, top, right, bottom)
                    divider.draw(c)
                }
            }
        }
    }
}