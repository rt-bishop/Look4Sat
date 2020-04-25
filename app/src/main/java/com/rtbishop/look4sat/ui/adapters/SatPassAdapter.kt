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
import com.rtbishop.look4sat.ui.fragments.PassListFragmentDirections
import java.text.SimpleDateFormat
import java.util.*

class SatPassAdapter(private var satPassList: MutableList<SatPass>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            val context: Context = itemView.context
            if (satPass.active) binding.passLeoImg.setImageResource(R.drawable.ic_pass_active)
            else binding.passLeoImg.setImageResource(R.drawable.ic_pass_inactive)

            binding.passLeoSatName.text = satPass.tle.name
            binding.passLeoSatId.text =
                String.format(context.getString(R.string.pass_satId), satPass.tle.catnum)
            binding.passLeoMaxEl.text =
                String.format(context.getString(R.string.pass_maxEl), satPass.pass.maxEl)
            binding.passLeoAosAz.text =
                String.format(context.getString(R.string.pass_aos_az), satPass.pass.aosAzimuth)
            binding.passLeoLosAz.text =
                String.format(context.getString(R.string.pass_los_az), satPass.pass.losAzimuth)
            binding.passLeoAosTime.text =
                SimpleDateFormat(context.getString(R.string.pass_dateTime), Locale.getDefault())
                    .format(satPass.pass.startTime)
            binding.passLeoLosTime.text =
                SimpleDateFormat(context.getString(R.string.pass_dateTime), Locale.getDefault())
                    .format(satPass.pass.endTime)
            binding.passLeoProgress.progress = satPass.progress

            itemView.setOnClickListener {
                val passIndex = satPassList.indexOf(satPass)
                val action = PassListFragmentDirections.actionPassToPolar(passIndex)
                itemView.findNavController().navigate(action)
            }
        }
    }

    inner class SatPassGeoHolder(itemView: View, private val binding: ItemPassGeoBinding) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(satPass: SatPass) {
            val context: Context = itemView.context
            val satPos = satPass.predictor.getSatPos(satPass.pass.startTime)
            val azimuth = satPos.azimuth * 180 / Math.PI

            binding.passGeoName.text = satPass.tle.name
            binding.passGeoId.text =
                String.format(context.getString(R.string.pass_satId), satPass.tle.catnum)
            binding.passGeoAz.text = String.format(context.getString(R.string.pat_azimuth), azimuth)
            binding.passGeoEl.text =
                String.format(context.getString(R.string.pat_elevation), satPass.pass.maxEl)

            itemView.setOnClickListener {
                val passIndex = satPassList.indexOf(satPass)
                val action = PassListFragmentDirections.actionPassToPolar(passIndex)
                itemView.findNavController().navigate(action)
            }
        }
    }
}
