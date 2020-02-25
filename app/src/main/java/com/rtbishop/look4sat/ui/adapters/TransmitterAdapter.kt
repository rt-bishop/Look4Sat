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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.repo.Transmitter

class TransmitterAdapter : RecyclerView.Adapter<TransmitterAdapter.TransmitterHolder>() {

    private var transmittersList = emptyList<Transmitter>()

    fun setList(list: List<Transmitter>) {
        transmittersList = list
    }

    override fun getItemCount(): Int {
        return transmittersList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransmitterHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.card_trans, parent, false)
        return TransmitterHolder(itemView)
    }

    override fun onBindViewHolder(holder: TransmitterHolder, position: Int) {
        holder.bind(transmittersList[position])
    }

    inner class TransmitterHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val context: Context = itemView.context
        private val description = itemView.findViewById<TextView>(R.id.trans_description)
        private val downlink = itemView.findViewById<TextView>(R.id.trans_downlink)
        private val uplink = itemView.findViewById<TextView>(R.id.trans_uplink)
        private val mode = itemView.findViewById<TextView>(R.id.trans_mode)
        private val inverted = itemView.findViewById<TextView>(R.id.trans_inverted)
        private val freqDivider = 1000000f

        fun bind(transmitter: Transmitter) {
            description.text = transmitter.description

            if (transmitter.downlinkLow != null && transmitter.downlinkHigh == null) {
                downlink.text = String.format(
                    context.getString(R.string.trans_down_low),
                    transmitter.downlinkLow / freqDivider
                )
            } else if (transmitter.downlinkLow != null && transmitter.downlinkHigh != null) {
                downlink.text = String.format(
                    context.getString(R.string.trans_down_lowHigh),
                    transmitter.downlinkLow / freqDivider,
                    transmitter.downlinkHigh / freqDivider
                )
            } else {
                downlink.text = context.getString(R.string.no_downlink)
            }

            if (transmitter.uplinkLow != null && transmitter.uplinkHigh == null) {
                uplink.text = String.format(
                    context.getString(R.string.trans_up_low),
                    transmitter.uplinkLow / freqDivider
                )
            } else if (transmitter.uplinkLow != null && transmitter.uplinkHigh != null) {
                uplink.text = String.format(
                    context.getString(R.string.trans_up_lowHigh),
                    transmitter.uplinkLow / freqDivider,
                    transmitter.uplinkHigh / freqDivider
                )
            } else {
                uplink.text = context.getString(R.string.no_uplink)
            }

            if (transmitter.mode != null) {
                mode.text =
                    String.format(context.getString(R.string.trans_mode), transmitter.mode)
            } else {
                mode.text = context.getString(R.string.no_mode)
            }
            if (transmitter.isInverted) {
                inverted.text = String.format(
                    context.getString(R.string.trans_inverted),
                    context.getString(R.string.btn_yes)
                )
            } else {
                inverted.text = String.format(
                    context.getString(R.string.trans_inverted),
                    context.getString(R.string.btn_no)
                )
            }
        }
    }
}
