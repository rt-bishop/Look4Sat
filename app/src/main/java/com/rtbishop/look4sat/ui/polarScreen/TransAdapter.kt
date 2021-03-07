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
package com.rtbishop.look4sat.ui.polarScreen

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.data.SatTrans
import com.rtbishop.look4sat.databinding.ItemTransBinding
import java.util.*

class TransAdapter(context: Context, private val satPass: SatPass) :
    RecyclerView.Adapter<TransAdapter.TransHolder>() {

    private val divider = 1000000f
    private val strNo = context.getString(R.string.btn_no)
    private val strYes = context.getString(R.string.btn_yes)
    private val mode = context.getString(R.string.trans_mode)
    private val formatLink = context.getString(R.string.trans_link_low)
    private val formatLinkNull = context.getString(R.string.trans_no_link)
    private val isInverted = context.getString(R.string.trans_inverted)
    private val predictor = satPass.predictor
    private var transmittersList = emptyList<SatTrans>()

    fun setData(list: List<SatTrans>) {
        transmittersList = list
        notifyDataSetChanged()
    }

    fun tickTransmitters() {
        if (!satPass.tle.isDeepspace) {
            val iterator = transmittersList.listIterator()
            while (iterator.hasNext()) {
                val trans = iterator.next()
                val index = transmittersList.indexOf(trans)
                notifyItemChanged(index)
            }
        }
    }

    override fun getItemCount(): Int {
        return transmittersList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransHolder {
        val binding = ItemTransBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return TransHolder(binding)
    }

    override fun onBindViewHolder(holder: TransHolder, position: Int) {
        holder.bind(transmittersList[position])
    }

    inner class TransHolder(private val binding: ItemTransBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(satTrans: SatTrans) {
            binding.description.text = satTrans.description

            if (satPass.tle.isDeepspace) setRegularFreq(satTrans)
            else setDopplerFreq(satTrans)

            if (satTrans.mode != null) binding.mode.text = String.format(mode, satTrans.mode)
            else binding.mode.text = String.format(mode, strNo)

            if (satTrans.isInverted) binding.isInverted.text = String.format(isInverted, strYes)
            else binding.isInverted.text = String.format(isInverted, strNo)
        }

        private fun setRegularFreq(satTrans: SatTrans) {
            if (satTrans.downlinkLow != null) {
                val downFreq = satTrans.downlinkLow / divider
                binding.downlink.text = String.format(Locale.ENGLISH, formatLink, downFreq)
            } else binding.downlink.text = formatLinkNull

            if (satTrans.uplinkLow != null) {
                val upFreq = satTrans.uplinkLow / divider
                binding.uplink.text = String.format(Locale.ENGLISH, formatLink, upFreq)
            } else binding.uplink.text = formatLinkNull
        }

        private fun setDopplerFreq(satTrans: SatTrans) {
            val dateNow = Date()

            if (satTrans.downlinkLow != null) {
                val downFreq = predictor.getDownlinkFreq(satTrans.downlinkLow, dateNow) / divider
                binding.downlink.text = String.format(Locale.ENGLISH, formatLink, downFreq)
            } else binding.downlink.text = formatLinkNull

            if (satTrans.uplinkLow != null) {
                val upFreq = predictor.getUplinkFreq(satTrans.uplinkLow, dateNow) / divider
                binding.uplink.text = String.format(Locale.ENGLISH, formatLink, upFreq)
            } else binding.uplink.text = formatLinkNull
        }
    }
}
