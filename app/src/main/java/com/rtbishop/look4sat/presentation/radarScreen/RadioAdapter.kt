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
package com.rtbishop.look4sat.presentation.radarScreen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.ItemRadioBinding
import com.rtbishop.look4sat.domain.model.SatRadio
import java.util.*

class RadioAdapter : RecyclerView.Adapter<RadioAdapter.TransHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<SatRadio>() {
        override fun areItemsTheSame(oldItem: SatRadio, newItem: SatRadio): Boolean {
            return oldItem.uuid == newItem.uuid
        }

        override fun areContentsTheSame(oldItem: SatRadio, newItem: SatRadio): Boolean {
            return oldItem.downlink == newItem.downlink
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(items: List<SatRadio>) = differ.submitList(items)

    override fun getItemCount() = differ.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransHolder {
        return TransHolder.from(parent)
    }

    override fun onBindViewHolder(holder: TransHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    class TransHolder private constructor(private val binding: ItemRadioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val divider = 1000000f
        private val stringYes = itemView.context.getString(R.string.radio_string_yes)
        private val stringNo = itemView.context.getString(R.string.radio_string_no)
        private val link = itemView.context.getString(R.string.radio_link_low)
        private val linkNull = itemView.context.getString(R.string.radio_no_link)
        private val mode = itemView.context.getString(R.string.radio_mode)
        private val inverted = itemView.context.getString(R.string.radio_inverted)

        fun bind(radio: SatRadio) {
            binding.run {
                radioInfo.text = radio.info
                radio.downlink.let { downlink ->
                    if (downlink != null) {
                        radioDownlink.text = String.format(Locale.ENGLISH, link, downlink / divider)
                    } else {
                        radioDownlink.text = linkNull
                    }
                }
                radio.uplink.let { uplink ->
                    if (uplink != null) {
                        radioUplink.text = String.format(Locale.ENGLISH, link, uplink / divider)
                    } else {
                        radioUplink.text = linkNull
                    }
                }
                if (radio.mode != null) {
                    radioMode.text = String.format(mode, radio.mode)
                } else {
                    radioMode.text = String.format(mode, stringNo)
                }
                if (radio.isInverted) {
                    radioInverted.text = String.format(inverted, stringYes)
                } else {
                    radioInverted.text = String.format(inverted, stringNo)
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): TransHolder {
                val inflater = LayoutInflater.from(parent.context)
                return TransHolder(ItemRadioBinding.inflate(inflater, parent, false))
            }
        }
    }
}
