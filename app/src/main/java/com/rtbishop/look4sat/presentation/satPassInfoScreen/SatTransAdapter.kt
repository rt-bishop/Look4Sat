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
package com.rtbishop.look4sat.presentation.satPassInfoScreen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.ItemTransBinding
import com.rtbishop.look4sat.domain.Transmitter
import java.util.*

class SatTransAdapter : RecyclerView.Adapter<SatTransAdapter.TransHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<Transmitter>() {
        override fun areItemsTheSame(oldItem: Transmitter, newItem: Transmitter): Boolean {
            return oldItem.uuid == newItem.uuid
        }

        override fun areContentsTheSame(oldItem: Transmitter, newItem: Transmitter): Boolean {
            return oldItem.downlink == newItem.downlink
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(transmitters: List<Transmitter>) {
        differ.submitList(transmitters)
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

        fun bind(transmitter: Transmitter) {
            binding.description.text = transmitter.info

            transmitter.downlink.let { downlink ->
                if (downlink != null) {
                    val downlinkFreq = downlink / divider
                    binding.downlink.text = String.format(Locale.ENGLISH, formatLink, downlinkFreq)
                } else binding.downlink.text = formatLinkNull
            }

            transmitter.uplink.let { uplink ->
                if (uplink != null) {
                    val uplinkFreq = uplink / divider
                    binding.uplink.text = String.format(Locale.ENGLISH, formatLink, uplinkFreq)
                } else binding.uplink.text = formatLinkNull
            }

            if (transmitter.mode != null) binding.mode.text = String.format(mode, transmitter.mode)
            else binding.mode.text = String.format(mode, strNo)

            if (transmitter.isInverted) binding.isInverted.text = String.format(isInverted, strYes)
            else binding.isInverted.text = String.format(isInverted, strNo)
        }

        companion object {
            fun from(parent: ViewGroup): TransHolder {
                val inflater = LayoutInflater.from(parent.context)
                return TransHolder(ItemTransBinding.inflate(inflater, parent, false))
            }
        }
    }
}
