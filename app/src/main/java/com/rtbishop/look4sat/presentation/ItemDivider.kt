/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.presentation

import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ItemDivider(private val resId: Int) : RecyclerView.ItemDecoration() {

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView) {
        ContextCompat.getDrawable(parent.context, resId)?.let { drawable ->
            val leftPos = parent.paddingLeft
            val rightPos = parent.width - parent.paddingRight
            parent.adapter?.itemCount?.let { childCount ->
                for (i in 0 until childCount) {
                    if (i == (childCount - 1)) continue
                    parent.getChildAt(i)?.let { view ->
                        val params = view.layoutParams as RecyclerView.LayoutParams
                        val topPos = view.bottom + params.bottomMargin
                        val bottomPos = topPos + drawable.intrinsicHeight
                        drawable.setBounds(leftPos, topPos, rightPos, bottomPos)
                        drawable.draw(canvas)
                    }
                }
            }
        }
    }
}
