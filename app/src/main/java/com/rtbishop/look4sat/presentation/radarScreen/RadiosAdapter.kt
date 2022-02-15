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
package com.rtbishop.look4sat.presentation.radarScreen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.ItemTransBinding
import com.rtbishop.look4sat.domain.model.SatRadio
import java.util.*

class RadiosAdapter : RecyclerView.Adapter<RadiosAdapter.TransHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<SatRadio>() {
        override fun areItemsTheSame(oldItem: SatRadio, newItem: SatRadio): Boolean {
            return oldItem.uuid == newItem.uuid
        }

        override fun areContentsTheSame(oldItem: SatRadio, newItem: SatRadio): Boolean {
            return oldItem.downlink == newItem.downlink
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(radios: List<SatRadio>) {
        differ.submitList(radios)
    }

    override fun getItemCount() = differ.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransHolder {
        return TransHolder.from(parent)
    }

    override fun onBindViewHolder(holder: TransHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    class TransHolder private constructor(private val binding: ItemTransBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val divider = 1000000f
        private val strNo = itemView.context.getString(R.string.no)
        private val strYes = itemView.context.getString(R.string.yes)
        private val mode = itemView.context.getString(R.string.trans_mode)
        private val formatLink = itemView.context.getString(R.string.trans_link_low)
        private val formatLinkNull = itemView.context.getString(R.string.trans_no_link)
        private val isInverted = itemView.context.getString(R.string.trans_inverted)

        fun bind(radio: SatRadio) {
            binding.transDesc.text = radio.info

            radio.downlink.let { downlink ->
                if (downlink != null) {
                    val downlinkFreq = downlink / divider
                    binding.transDownlink.text =
                        String.format(Locale.ENGLISH, formatLink, downlinkFreq)
                } else {
                    binding.transDownlink.text = formatLinkNull
                }
            }

            radio.uplink.let { uplink ->
                if (uplink != null) {
                    val uplinkFreq = uplink / divider
                    binding.transUplink.text = String.format(Locale.ENGLISH, formatLink, uplinkFreq)
                } else {
                    binding.transUplink.text = formatLinkNull
                }
            }

            if (radio.mode != null) {
                binding.transMode.text = String.format(mode, radio.mode)
            } else {
                binding.transMode.text = String.format(mode, strNo)
            }

            if (radio.isInverted) {
                binding.transInverted.text = String.format(isInverted, strYes)
            } else {
                binding.transInverted.text = String.format(isInverted, strNo)
            }
        }

        companion object {
            fun from(parent: ViewGroup): TransHolder {
                val inflater = LayoutInflater.from(parent.context)
                return TransHolder(ItemTransBinding.inflate(inflater, parent, false))
            }
        }
    }
}
