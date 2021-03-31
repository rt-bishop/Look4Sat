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
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.SatPass
import com.rtbishop.look4sat.databinding.ItemPassGeoBinding
import com.rtbishop.look4sat.databinding.ItemPassLeoBinding
import java.text.SimpleDateFormat
import java.util.*

class PassesAdapter(private val shouldUseUTC: Boolean = false) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<SatPass>() {
        override fun areItemsTheSame(oldItem: SatPass, newItem: SatPass): Boolean {
            return oldItem.catNum == newItem.catNum && oldItem.startDate == newItem.startDate
        }

        override fun areContentsTheSame(oldItem: SatPass, newItem: SatPass): Boolean {
            return false
        }
    }
    private val listDiffer = AsyncListDiffer(this, diffCallback)
    private lateinit var passesClickListener: PassesClickListener

    interface PassesClickListener {
        fun navigateToPass(satPass: SatPass)
    }

    fun setPassesClickListener(listener: PassesClickListener) {
        passesClickListener = listener
    }

    fun submitPasses(items: List<SatPass>) {
        listDiffer.submitList(items)
    }

    override fun getItemCount() = listDiffer.currentList.size

    override fun getItemViewType(position: Int): Int {
        return if (listDiffer.currentList[position].isDeepSpace) 1
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
            (holder as SatPassLeoHolder)
                .bind(listDiffer.currentList[position], shouldUseUTC, passesClickListener)
        } else {
            (holder as SatPassGeoHolder)
                .bind(listDiffer.currentList[position], passesClickListener)
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

        fun bind(satPass: SatPass, shouldUseUTC: Boolean, listener: PassesClickListener) {
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
                listener.navigateToPass(satPass)
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

        fun bind(satPass: SatPass, listener: PassesClickListener) {
            binding.apply {
                passGeoSatName.text = satPass.name
                passGeoSatId.text = String.format(satIdFormat, satPass.catNum)
                passGeoAz.text = String.format(azFormat, satPass.tcaAzimuth)
                passGeoAlt.text = String.format(altFormat, satPass.altitude)
                passGeoEl.text = String.format(elevFormat, satPass.maxElevation)
            }
            itemView.setOnClickListener {
                listener.navigateToPass(satPass)
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
