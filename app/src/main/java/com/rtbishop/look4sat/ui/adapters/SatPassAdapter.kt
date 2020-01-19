/*
 * Look4Sat. Amateur radio & weather satellites passes calculator for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.repo.SatPass
import com.rtbishop.look4sat.ui.PassListFragmentDirections
import java.text.SimpleDateFormat
import java.util.*

class SatPassAdapter : RecyclerView.Adapter<SatPassAdapter.SatPassHolder>() {

    private var satPassList = mutableListOf<SatPass>()

    fun setList(list: MutableList<SatPass>) {
        satPassList = list
        notifyDataSetChanged()
    }

    fun updateRecycler() {
        val timeNow = Date()
        val iterator = satPassList.listIterator()
        while (iterator.hasNext()) {
            val satPass = iterator.next()
            if (satPass.progress < 100) {
                val timeStart = satPass.pass.startTime
                val timeEnd = satPass.pass.endTime
                if (timeNow.after(timeStart)) {
                    satPass.active = true
                    val index = satPassList.indexOf(satPass)
                    val deltaTotal = timeEnd.time.minus(timeStart.time)
                    val deltaNow = timeNow.time.minus(timeStart.time)
                    satPass.progress = ((deltaNow.toFloat() / deltaTotal.toFloat()) * 100).toInt()
                    notifyItemChanged(index)
                }
            } else {
                val index = satPassList.indexOf(satPass)
                iterator.remove()
                notifyItemRemoved(index)
            }
        }
    }

    override fun getItemCount(): Int {
        return satPassList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatPassHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.card_pass, parent, false)
        return SatPassHolder(itemView)
    }

    override fun onBindViewHolder(holder: SatPassHolder, position: Int) {
        holder.bind(satPassList[position])
    }

    inner class SatPassHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val context: Context = itemView.context
        private val passImg = itemView.findViewById<ImageView>(R.id.pass_img)
        private val satName = itemView.findViewById<TextView>(R.id.pass_satName)
        private val satId = itemView.findViewById<TextView>(R.id.pass_satId)
        private val maxEl = itemView.findViewById<TextView>(R.id.pass_maxEl)
        private val aosAz = itemView.findViewById<TextView>(R.id.pass_aosAz)
        private val losAz = itemView.findViewById<TextView>(R.id.pass_losAz)
        private val aosTime = itemView.findViewById<TextView>(R.id.pass_aosTime)
        private val losTime = itemView.findViewById<TextView>(R.id.pass_losTime)
        private var progressBar = itemView.findViewById<ProgressBar>(R.id.pass_progress)

        fun bind(satPass: SatPass) {

            if (satPass.active) passImg.setImageResource(R.drawable.ic_pass_active)
            else passImg.setImageResource(R.drawable.ic_pass_inactive)

            satName.text = satPass.tle.name
            satId.text =
                String.format(context.getString(R.string.pass_satId), satPass.tle.catnum)
            maxEl.text =
                String.format(context.getString(R.string.pass_maxEl), satPass.pass.maxEl)
            aosAz.text =
                String.format(context.getString(R.string.pass_aos_az), satPass.pass.aosAzimuth)
            losAz.text =
                String.format(context.getString(R.string.pass_los_az), satPass.pass.losAzimuth)
            aosTime.text =
                SimpleDateFormat(context.getString(R.string.pass_dateTime), Locale.getDefault())
                    .format(satPass.pass.startTime)
            losTime.text =
                SimpleDateFormat(context.getString(R.string.pass_dateTime), Locale.getDefault())
                    .format(satPass.pass.endTime)
            progressBar.progress = satPass.progress

            itemView.setOnClickListener {
                val action = PassListFragmentDirections.actionPassToPolar(satPass)
                itemView.findNavController().navigate(action)
            }
        }
    }
}