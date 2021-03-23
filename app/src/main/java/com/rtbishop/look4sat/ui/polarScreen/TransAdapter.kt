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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.SatPass
import com.rtbishop.look4sat.data.model.SatTrans
import com.rtbishop.look4sat.databinding.ItemTransBinding
import com.rtbishop.look4sat.utility.PassPredictor
import java.util.*

class TransAdapter(private val pass: SatPass) : RecyclerView.Adapter<TransAdapter.TransHolder>() {

    private var transmittersList = emptyList<SatTrans>()

    fun setData(list: List<SatTrans>) {
        transmittersList = list
        notifyDataSetChanged()
    }

    fun tickTransmitters() {
        if (!pass.isDeepSpace) {
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return transmittersList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransHolder {
        return TransHolder.from(parent)
    }

    override fun onBindViewHolder(holder: TransHolder, position: Int) {
        holder.bind(transmittersList[position], pass)
    }

    class TransHolder private constructor(private val binding: ItemTransBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateNow = Date()
        private val divider = 1000000f
        private val strNo = itemView.context.getString(R.string.btn_no)
        private val strYes = itemView.context.getString(R.string.btn_yes)
        private val mode = itemView.context.getString(R.string.trans_mode)
        private val formatLink = itemView.context.getString(R.string.trans_link_low)
        private val formatLinkNull = itemView.context.getString(R.string.trans_no_link)
        private val isInverted = itemView.context.getString(R.string.trans_inverted)

        fun bind(satTrans: SatTrans, satPass: SatPass) {
            binding.description.text = satTrans.info

            if (satPass.isDeepSpace) setRegularFreq(satTrans)
            else setDopplerFreq(satTrans, satPass.predictor)

            if (satTrans.mode != null) binding.mode.text = String.format(mode, satTrans.mode)
            else binding.mode.text = String.format(mode, strNo)

            if (satTrans.isInverted) binding.isInverted.text = String.format(isInverted, strYes)
            else binding.isInverted.text = String.format(isInverted, strNo)
        }

        private fun setRegularFreq(satTrans: SatTrans) {
            if (satTrans.downlink != null) {
                val downFreq = satTrans.downlink / divider
                binding.downlink.text = String.format(Locale.ENGLISH, formatLink, downFreq)
            } else binding.downlink.text = formatLinkNull

            if (satTrans.uplink != null) {
                val upFreq = satTrans.uplink / divider
                binding.uplink.text = String.format(Locale.ENGLISH, formatLink, upFreq)
            } else binding.uplink.text = formatLinkNull
        }

        private fun setDopplerFreq(satTrans: SatTrans, predictor: PassPredictor) {
            if (satTrans.downlink != null) {
                val downlink = predictor.getDownlinkFreq(satTrans.downlink, dateNow) / divider
                binding.downlink.text = String.format(Locale.ENGLISH, formatLink, downlink)
            } else binding.downlink.text = formatLinkNull

            if (satTrans.uplink != null) {
                val uplink = predictor.getUplinkFreq(satTrans.uplink, dateNow) / divider
                binding.uplink.text = String.format(Locale.ENGLISH, formatLink, uplink)
            } else binding.uplink.text = formatLinkNull
        }

        companion object {
            fun from(parent: ViewGroup): TransHolder {
                val inflater = LayoutInflater.from(parent.context)
                return TransHolder(ItemTransBinding.inflate(inflater, parent, false))
            }
        }
    }
}
