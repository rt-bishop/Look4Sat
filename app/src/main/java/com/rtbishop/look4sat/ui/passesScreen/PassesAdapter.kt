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
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.SatPass
import com.rtbishop.look4sat.databinding.ItemPassGeoBinding
import com.rtbishop.look4sat.databinding.ItemPassLeoBinding
import com.rtbishop.look4sat.utility.navigateSafe
import java.text.SimpleDateFormat
import java.util.*

class PassesAdapter(private val isUTC: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<SatPass>() {
        override fun areItemsTheSame(oldItem: SatPass, newItem: SatPass): Boolean {
            return oldItem.catNum == newItem.catNum && oldItem.startDate == newItem.startDate
        }

        override fun areContentsTheSame(oldItem: SatPass, newItem: SatPass): Boolean {
            return oldItem.progress == newItem.progress
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)
    private var allPasses = emptyList<SatPass>()
    private var currentPasses = emptyList<SatPass>()

    fun submitList(passes: List<SatPass>) {
        allPasses = passes.map { it.copy() }
        currentPasses = passes.map { it.copy() }
        tickDiffer(System.currentTimeMillis())
    }

    fun tickDiffer(timeNow: Long) {
        val list = currentPasses.map { it.copy() }
        list.forEach { pass ->
            if (!pass.isDeepSpace) {
                val timeStart = pass.startDate.time
                if (timeNow > timeStart) {
                    val timeEnd = pass.endDate.time
                    val deltaNow = timeNow.minus(timeStart).toFloat()
                    val deltaTotal = timeEnd.minus(timeStart).toFloat()
                    pass.progress = ((deltaNow / deltaTotal) * 100).toInt()
                }
            }
        }
        currentPasses = list.filter { pass -> pass.progress < 100 }
        differ.submitList(currentPasses)
    }

    override fun getItemCount() = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return if (differ.currentList[position].isDeepSpace) 1
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
        val pass = differ.currentList[position]
        if (holder.itemViewType == 0) {
            (holder as SatPassLeoHolder).bind(pass, differ.currentList.indexOf(pass), isUTC)
        } else {
            (holder as SatPassGeoHolder).bind(pass, differ.currentList.indexOf(pass))
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

        fun bind(satPass: SatPass, passIndex: Int, shouldUseUTC: Boolean) {
            binding.apply {
                if (shouldUseUTC) {
                    startFormat.timeZone = timeZoneUTC
                    endFormat.timeZone = timeZoneUTC
                }
                passLeoSatName.text = satPass.name
                passLeoSatId.text = String.format(satIdFormat, satPass.catNum)
                passLeoAosAz.text = String.format(aosAzFormat, satPass.aosAzimuth)
                passLeoMaxEl.text = String.format(maxElFormat, satPass.maxElevation)
                passLeoLosAz.text = String.format(losAzFormat, satPass.losAzimuth)
                passLeoStart.text = startFormat.format(satPass.startDate)
                passLeoAlt.text = String.format(altFormat, satPass.altitude)
                passLeoEnd.text = endFormat.format(satPass.endDate)
                passLeoProgress.progress = satPass.progress
            }

            itemView.setOnClickListener {
                if (satPass.progress < 100) {
                    val bundle = bundleOf("index" to passIndex)
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

        fun bind(satPass: SatPass, passIndex: Int) {
            binding.apply {
                passGeoSatName.text = satPass.name
                passGeoSatId.text = String.format(satIdFormat, satPass.catNum)
                passGeoAz.text = String.format(azFormat, satPass.tcaAzimuth)
                passGeoAlt.text = String.format(altFormat, satPass.altitude)
                passGeoEl.text = String.format(elevFormat, satPass.maxElevation)
            }

            itemView.setOnClickListener {
                val bundle = bundleOf("index" to passIndex)
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
