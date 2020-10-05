/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
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
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.ItemPassGeoBinding
import com.rtbishop.look4sat.databinding.ItemPassLeoBinding
import com.rtbishop.look4sat.ui.fragments.PassesFragmentDirections
import java.text.SimpleDateFormat
import java.util.*

class PassesAdapter(context: Context, private var shouldUseUTC: Boolean = false) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val satIdFormat = context.getString(R.string.pass_satId)
    private val azFormat = context.getString(R.string.pat_azimuth)
    private val elevFormat = context.getString(R.string.pat_elevation)
    private val aosAzFormat = context.getString(R.string.pass_aos_az)
    private val losAzFormat = context.getString(R.string.pass_los_az)
    private val dateFormat = context.getString(R.string.pass_dateTime)
    private val timeZoneUTC = TimeZone.getTimeZone("UTC")
    private val simpleDateFormat = SimpleDateFormat(dateFormat, Locale.getDefault())
    private var satPassList: MutableList<SatPass> = mutableListOf()

    fun setList(list: MutableList<SatPass>) {
        satPassList = list
        notifyDataSetChanged()
    }

    fun updateRecycler() {
        val timeNow = System.currentTimeMillis()
        val iterator = satPassList.listIterator()
        while (iterator.hasNext()) {
            val satPass = iterator.next()
            if (satPass.progress < 100) {
                val timeStart = satPass.pass.startTime.time
                val timeEnd = satPass.pass.endTime.time
                if (timeNow.minus(timeStart) > 0) {
                    satPass.active = true
                    val index = satPassList.indexOf(satPass)
                    val deltaTotal = timeEnd.minus(timeStart)
                    val deltaNow = timeNow.minus(timeStart)
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

    override fun getItemViewType(position: Int): Int {
        return if (satPassList[position].tle.isDeepspace) 1
        else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val bindingLeo = ItemPassLeoBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            SatPassLeoHolder(bindingLeo.root, bindingLeo)
        } else {
            val bindingGeo = ItemPassGeoBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            SatPassGeoHolder(bindingGeo.root, bindingGeo)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == 0) {
            (holder as SatPassLeoHolder).bind(satPassList[position])
        } else {
            (holder as SatPassGeoHolder).bind(satPassList[position])
        }
    }

    inner class SatPassLeoHolder(itemView: View, private val binding: ItemPassLeoBinding) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(satPass: SatPass) {
            binding.apply {
                if (satPass.active) passLeoImg.setImageResource(R.drawable.ic_pass_active)
                else passLeoImg.setImageResource(R.drawable.ic_pass_inactive)

                passLeoSatName.text = satPass.tle.name
                passLeoSatId.text = String.format(satIdFormat, satPass.tle.catnum)
                passLeoMaxEl.text = String.format(elevFormat, satPass.pass.maxEl)
                passLeoAosAz.text = String.format(aosAzFormat, satPass.pass.aosAzimuth)
                passLeoLosAz.text = String.format(losAzFormat, satPass.pass.losAzimuth)
                passLeoProgress.progress = satPass.progress

                if (shouldUseUTC) simpleDateFormat.timeZone = timeZoneUTC
                passLeoAosTime.text = simpleDateFormat.format(satPass.pass.startTime)
                passLeoLosTime.text = simpleDateFormat.format(satPass.pass.endTime)
            }

            itemView.setOnClickListener {
                val passIndex = satPassList.indexOf(satPass)
                val action = PassesFragmentDirections.actionPassToPolar(passIndex)
                itemView.findNavController().navigate(action)
            }
        }
    }

    inner class SatPassGeoHolder(itemView: View, private val binding: ItemPassGeoBinding) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(satPass: SatPass) {
            val satPos = satPass.predictor.getSatPos(satPass.pass.startTime)
            val azimuth = Math.toDegrees(satPos.azimuth)

            binding.apply {
                passGeoName.text = satPass.tle.name
                passGeoId.text = String.format(satIdFormat, satPass.tle.catnum)
                passGeoAz.text = String.format(azFormat, azimuth)
                passGeoEl.text = String.format(elevFormat, satPass.pass.maxEl)
            }

            itemView.setOnClickListener {
                val passIndex = satPassList.indexOf(satPass)
                val action = PassesFragmentDirections.actionPassToPolar(passIndex)
                itemView.findNavController().navigate(action)
            }
        }
    }
}
