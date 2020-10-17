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

class TransAdapter(context: Context) : RecyclerView.Adapter<TransAdapter.TransHolder>() {

    private lateinit var predictor: PassPredictor
    private val divider = 1000000f
    private val strNo = context.getString(R.string.btn_no)
    private val strYes = context.getString(R.string.btn_yes)
    private val mode = context.getString(R.string.trans_mode)
    private val formatLink = context.getString(R.string.trans_down_low_doppler)
    private val formatLinkNull = context.getString(R.string.no_downlink_doppler)
    private val isInverted = context.getString(R.string.trans_inverted)
    private var transmittersList = emptyList<SatTrans>()

    fun setPredictor(predictor: PassPredictor) {
        this.predictor = predictor
    }

    fun setList(list: List<SatTrans>) {
        transmittersList = list
        notifyDataSetChanged()
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
            val dateNow = Date()
            binding.description.text = satTrans.description

            if (satTrans.downlinkLow != null) {
                val downFreq = predictor.getDownlinkFreq(satTrans.downlinkLow, dateNow) / divider
                binding.downlink.text = String.format(formatLink, downFreq)
            } else binding.downlink.text = formatLinkNull

            if (satTrans.uplinkLow != null) {
                val upFreq = predictor.getUplinkFreq(satTrans.uplinkLow, dateNow) / divider
                binding.uplink.text = String.format(formatLink, upFreq)
            } else binding.uplink.text = formatLinkNull

            if (satTrans.mode != null) binding.mode.text = String.format(mode, satTrans.mode)
            else binding.mode.text = String.format(mode, strNo)

            if (satTrans.isInverted) binding.isInverted.text = String.format(isInverted, strYes)
            else binding.isInverted.text = String.format(isInverted, strNo)
        }
    }
}
