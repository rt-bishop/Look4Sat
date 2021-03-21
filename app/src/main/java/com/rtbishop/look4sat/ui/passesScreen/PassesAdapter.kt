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
package com.rtbishop.look4sat.ui.passesScreen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.SatPass
import com.rtbishop.look4sat.databinding.ItemPassGeoBinding
import com.rtbishop.look4sat.databinding.ItemPassLeoBinding
import com.rtbishop.look4sat.utility.navigateSafe
import java.text.SimpleDateFormat
import java.util.*

class PassesAdapter(private val shouldUseUTC: Boolean = false) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var satPassList: MutableList<SatPass> = mutableListOf()

    fun setList(list: MutableList<SatPass>) {
        satPassList = list
        notifyDataSetChanged()
    }

    fun tickPasses(timeNow: Long) {
        val iterator = satPassList.listIterator()
        while (iterator.hasNext()) {
            val satPass = iterator.next()
            if (!satPass.satellite.tle.isDeepspace) {
                if (satPass.progress < 100) {
                    val timeStart = satPass.pass.startTime.time
                    if (timeNow > timeStart) {
                        val timeEnd = satPass.pass.endTime.time
                        val index = satPassList.indexOf(satPass)
                        val deltaNow = timeNow.minus(timeStart).toFloat()
                        val deltaTotal = timeEnd.minus(timeStart).toFloat()
                        satPass.progress = ((deltaNow / deltaTotal) * 100).toInt()
                        notifyItemChanged(index)
                    }
                } else {
                    val index = satPassList.indexOf(satPass)
                    iterator.remove()
                    notifyItemRemoved(index)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return satPassList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (satPassList[position].satellite.tle.isDeepspace) 1
        else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            SatPassLeoHolder.from(parent)
        } else {
            SatPassGeoHolder.from(parent)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == 0) {
            (holder as SatPassLeoHolder).bind(satPassList[position], position, shouldUseUTC)
        } else {
            (holder as SatPassGeoHolder).bind(satPassList[position], position)
        }
    }

    class SatPassLeoHolder private constructor(private val binding: ItemPassLeoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val satIdFormat = itemView.context.getString(R.string.pass_satId)
        private val altFormat = itemView.context.getString(R.string.pat_altitude)
        private val aosAzFormat = itemView.context.getString(R.string.pass_aosAz)
        private val maxElFormat = itemView.context.getString(R.string.pass_maxEl)
        private val losAzFormat = itemView.context.getString(R.string.pass_losAz)
        private val startTimeFormat = itemView.context.getString(R.string.pass_startTime)
        private val endTimeFormat = itemView.context.getString(R.string.pass_endTime)
        private val timeZoneUTC = TimeZone.getTimeZone("UTC")
        private val startFormat = SimpleDateFormat(startTimeFormat, Locale.getDefault())
        private val endFormat = SimpleDateFormat(endTimeFormat, Locale.getDefault())

        fun bind(satPass: SatPass, position: Int, shouldUseUTC: Boolean) {
            val satPos = satPass.predictor.getSatPos(Date())
            binding.apply {
                if (shouldUseUTC) {
                    startFormat.timeZone = timeZoneUTC
                    endFormat.timeZone = timeZoneUTC
                }
                passLeoSatName.text = satPass.satellite.tle.name
                passLeoSatId.text = String.format(satIdFormat, satPass.satellite.tle.catnum)
                passLeoAosAz.text = String.format(aosAzFormat, satPass.pass.aosAzimuth)
                passLeoMaxEl.text = String.format(maxElFormat, satPass.pass.maxEl)
                passLeoLosAz.text = String.format(losAzFormat, satPass.pass.losAzimuth)
                passLeoStart.text = startFormat.format(satPass.pass.startTime)
                passLeoAlt.text = String.format(altFormat, satPos.altitude)
                passLeoEnd.text = endFormat.format(satPass.pass.endTime)
                passLeoProgress.progress = satPass.progress
            }

            itemView.setOnClickListener {
                if (satPass.progress < 100) {
                    val bundle = bundleOf("index" to position)
                    itemView.findNavController().navigateSafe(R.id.action_passes_to_polar, bundle)
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): SatPassLeoHolder {
                val inflater = LayoutInflater.from(parent.context)
                return SatPassLeoHolder(ItemPassLeoBinding.inflate(inflater, parent, false))
            }
        }
    }

    class SatPassGeoHolder private constructor(private val binding: ItemPassGeoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val satIdFormat = itemView.context.getString(R.string.pass_satId)
        private val azFormat = itemView.context.getString(R.string.pat_azimuth)
        private val altFormat = itemView.context.getString(R.string.pat_altitude)
        private val elevFormat = itemView.context.getString(R.string.pat_elevation)

        fun bind(satPass: SatPass, position: Int) {
            val satPos = satPass.predictor.getSatPos(Date())
            binding.apply {
                passGeoSatName.text = satPass.satellite.tle.name
                passGeoSatId.text = String.format(satIdFormat, satPass.satellite.tle.catnum)
                passGeoAz.text = String.format(azFormat, Math.toDegrees(satPos.azimuth))
                passGeoAlt.text = String.format(altFormat, satPos.altitude)
                passGeoEl.text = String.format(elevFormat, Math.toDegrees(satPos.elevation))
            }

            itemView.setOnClickListener {
                val bundle = bundleOf("index" to position)
                itemView.findNavController().navigateSafe(R.id.action_passes_to_polar, bundle)
            }
        }

        companion object {
            fun from(parent: ViewGroup): SatPassGeoHolder {
                val inflater = LayoutInflater.from(parent.context)
                return SatPassGeoHolder(ItemPassGeoBinding.inflate(inflater, parent, false))
            }
        }
    }
}
