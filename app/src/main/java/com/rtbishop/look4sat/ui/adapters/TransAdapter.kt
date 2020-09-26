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
import com.rtbishop.look4sat.data.SatTrans
import com.rtbishop.look4sat.databinding.ItemTransBinding
import com.rtbishop.look4sat.utility.PassPredictor
import java.util.*

class TransAdapter : RecyclerView.Adapter<TransAdapter.TransHolder>() {

    private var transmittersList = emptyList<SatTrans>()
    private lateinit var predictor: PassPredictor

    fun setPredictor(predict: PassPredictor) {
        predictor = predict
    }

    fun setList(list: List<SatTrans>) {
        transmittersList = list
    }

    override fun getItemCount(): Int {
        return transmittersList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransHolder {
        val binding = ItemTransBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return TransHolder(binding.root, binding)
    }

    override fun onBindViewHolder(holder: TransHolder, position: Int) {
        holder.bind(transmittersList[position])
    }

    inner class TransHolder(itemView: View, private val binding: ItemTransBinding) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(satTrans: SatTrans) {
            val context: Context = itemView.context
            val freqDivider = 1000000f

            binding.transDescription.text = satTrans.description

            if (satTrans.downlinkLow != null && satTrans.downlinkHigh == null) {
                binding.transDownlink.text = String
                    .format(
                        context.getString(R.string.trans_down_low),
                        satTrans.downlinkLow / freqDivider
                    )
                binding.transDownlinkDoppler.text = String
                    .format(
                        context.getString(R.string.trans_down_low_doppler),
                        predictor.getDownlinkFreq(satTrans.downlinkLow, Date()) / freqDivider
                    )
            } else if (satTrans.downlinkLow != null && satTrans.downlinkHigh != null) {
                binding.transDownlink.text = String
                    .format(
                        context.getString(R.string.trans_down_lowHigh),
                        satTrans.downlinkLow / freqDivider,
                        satTrans.downlinkHigh / freqDivider
                    )
                binding.transDownlinkDoppler.text = String
                    .format(
                        context.getString(R.string.trans_down_lowHigh_doppler),
                        predictor.getDownlinkFreq(satTrans.downlinkLow, Date()) / freqDivider,
                        predictor.getDownlinkFreq(satTrans.downlinkHigh, Date()) / freqDivider
                    )
            } else {
                binding.transDownlink.text = context.getString(R.string.no_downlink)
                binding.transDownlinkDoppler.text = context.getString(R.string.no_downlink_doppler)
            }

            if (satTrans.uplinkLow != null && satTrans.uplinkHigh == null) {
                binding.transUplink.text = String.format(
                    context.getString(R.string.trans_up_low),
                    satTrans.uplinkLow / freqDivider
                )
                binding.transUplinkDoppler.text = String.format(
                    context.getString(R.string.trans_up_low_doppler),
                    predictor.getUplinkFreq(satTrans.uplinkLow, Date()) / freqDivider
                )
            } else if (satTrans.uplinkLow != null && satTrans.uplinkHigh != null) {
                binding.transUplink.text = String.format(
                    context.getString(R.string.trans_up_lowHigh),
                    satTrans.uplinkLow / freqDivider,
                    satTrans.uplinkHigh / freqDivider
                )
                binding.transUplinkDoppler.text = String.format(
                    context.getString(R.string.trans_up_lowHigh_doppler),
                    predictor.getUplinkFreq(satTrans.uplinkLow, Date()) / freqDivider,
                    predictor.getUplinkFreq(satTrans.uplinkHigh, Date()) / freqDivider
                )
            } else {
                binding.transUplink.text = context.getString(R.string.no_uplink)
                binding.transUplinkDoppler.text = context.getString(R.string.no_uplink_doppler)
            }

            if (satTrans.mode != null) {
                binding.transMode.text =
                    String.format(context.getString(R.string.trans_mode), satTrans.mode)
            } else {
                binding.transMode.text = context.getString(R.string.no_mode)
            }
            if (satTrans.isInverted) {
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
