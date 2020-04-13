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
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.Transmitter
import com.rtbishop.look4sat.databinding.ItemTransmitterBinding

class TransmitterAdapter : RecyclerView.Adapter<TransmitterAdapter.TransmitterHolder>() {

    private var transmittersList = emptyList<Transmitter>()

    fun setList(list: List<Transmitter>) {
        transmittersList = list
    }

    override fun getItemCount(): Int {
        return transmittersList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransmitterHolder {
        val binding = ItemTransmitterBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return TransmitterHolder(binding.root, binding)
    }

    override fun onBindViewHolder(holder: TransmitterHolder, position: Int) {
        holder.bind(transmittersList[position])
    }

    inner class TransmitterHolder(itemView: View, private val binding: ItemTransmitterBinding) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(transmitter: Transmitter) {
            val context: Context = itemView.context
            val freqDivider = 1000000f

            binding.transDescription.text = transmitter.description

            if (transmitter.downlinkLow != null && transmitter.downlinkHigh == null) {
                binding.transDownlink.text = String
                    .format(
                        context.getString(R.string.trans_down_low),
                        transmitter.downlinkLow / freqDivider
                    )
            } else if (transmitter.downlinkLow != null && transmitter.downlinkHigh != null) {
                binding.transDownlink.text = String
                    .format(
                        context.getString(R.string.trans_down_lowHigh),
                        transmitter.downlinkLow / freqDivider,
                        transmitter.downlinkHigh / freqDivider
                    )
            } else {
                binding.transDownlink.text = context.getString(R.string.no_downlink)
            }

            if (transmitter.uplinkLow != null && transmitter.uplinkHigh == null) {
                binding.transUplink.text = String.format(
                    context.getString(R.string.trans_up_low),
                    transmitter.uplinkLow / freqDivider
                )
            } else if (transmitter.uplinkLow != null && transmitter.uplinkHigh != null) {
                binding.transUplink.text = String.format(
                    context.getString(R.string.trans_up_lowHigh),
                    transmitter.uplinkLow / freqDivider,
                    transmitter.uplinkHigh / freqDivider
                )
            } else {
                binding.transUplink.text = context.getString(R.string.no_uplink)
            }

            if (transmitter.mode != null) {
                binding.transMode.text =
                    String.format(context.getString(R.string.trans_mode), transmitter.mode)
            } else {
                binding.transMode.text = context.getString(R.string.no_mode)
            }
            if (transmitter.isInverted) {
                binding.transInverted.text = String.format(
                    context.getString(R.string.trans_inverted),
                    context.getString(R.string.btn_yes)
                )
            } else {
                binding.transInverted.text = String.format(
                    context.getString(R.string.trans_inverted),
                    context.getString(R.string.btn_no)
                )
            }
        }
    }
}
