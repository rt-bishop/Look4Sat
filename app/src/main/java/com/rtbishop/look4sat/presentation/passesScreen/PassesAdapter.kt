/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.presentation.passesScreen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.ItemPassBinding
import com.rtbishop.look4sat.domain.predict.SatPass
import java.text.SimpleDateFormat
import java.util.*

class PassesAdapter(private val clickListener: PassesClickListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<SatPass>() {
        override fun areItemsTheSame(oldItem: SatPass, newItem: SatPass): Boolean {
            return oldItem.catNum == newItem.catNum && oldItem.aosTime == newItem.aosTime
        }

        override fun areContentsTheSame(oldItem: SatPass, newItem: SatPass): Boolean {
            return oldItem.progress == newItem.progress
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)
    private var isUTC: Boolean = false

    interface PassesClickListener {
        fun navigateToPass(satPass: SatPass)
    }

    fun submitList(passes: List<SatPass>) {
        differ.submitList(passes)
    }

    fun setUTC(isUTC: Boolean) {
        this.isUTC = isUTC
    }

    override fun getItemCount() = differ.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PassHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as PassHolder).bind(differ.currentList[position], clickListener, isUTC)
    }

    class PassHolder private constructor(private val binding: ItemPassBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val satIdFormat = itemView.context.getString(R.string.pass_satId)
        private val aosAzFormat = itemView.context.getString(R.string.pass_aosAz)
        private val altFormat = itemView.context.getString(R.string.pass_altitude)
        private val losAzFormat = itemView.context.getString(R.string.pass_los)
        private val startTimeFormat = itemView.context.getString(R.string.pass_startTime)
        private val elevFormat = itemView.context.getString(R.string.pass_elevation)
        private val endTimeFormat = itemView.context.getString(R.string.pass_endTime)
        private val placeholder = itemView.context.getString(R.string.pass_placeholder)
        private val timeZoneUTC = TimeZone.getTimeZone("UTC")
        private val startFormat = SimpleDateFormat(startTimeFormat, Locale.getDefault())
        private val endFormat = SimpleDateFormat(endTimeFormat, Locale.getDefault())

        fun bind(satPass: SatPass, listener: PassesClickListener, shouldUseUTC: Boolean) {
            binding.apply {
                if (satPass.isDeepSpace) {
                    passName.text = satPass.name
                    passId.text = String.format(satIdFormat, satPass.catNum)
                    passAos.text = String.format(aosAzFormat, satPass.aosAzimuth)
                    passAltitude.text = String.format(altFormat, satPass.altitude)
                    passLos.text = String.format(losAzFormat, satPass.losAzimuth)
                    passStart.text = placeholder
                    passElev.text = String.format(elevFormat, satPass.maxElevation)
                    passEnd.text = placeholder
                    passProgress.progress = 100
                } else {
                    if (shouldUseUTC) {
                        startFormat.timeZone = timeZoneUTC
                        endFormat.timeZone = timeZoneUTC
                    }
                    passName.text = satPass.name
                    passId.text = String.format(satIdFormat, satPass.catNum)
                    passAos.text = String.format(aosAzFormat, satPass.aosAzimuth)
                    passAltitude.text = String.format(altFormat, satPass.altitude)
                    passLos.text = String.format(losAzFormat, satPass.losAzimuth)
                    passStart.text = startFormat.format(Date(satPass.aosTime))
                    passElev.text = String.format(elevFormat, satPass.maxElevation)
                    passEnd.text = endFormat.format(Date(satPass.losTime))
                    passProgress.progress = satPass.progress
                }
            }

            itemView.setOnClickListener {
                listener.navigateToPass(satPass)
            }
        }

        companion object {
            fun from(parent: ViewGroup): PassHolder {
                val inflater = LayoutInflater.from(parent.context)
                return PassHolder(ItemPassBinding.inflate(inflater, parent, false))
            }
        }
    }
}
