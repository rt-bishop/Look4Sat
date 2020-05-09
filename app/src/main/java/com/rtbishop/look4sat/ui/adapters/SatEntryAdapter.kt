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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.databinding.ItemSatEntryBinding

class SatEntryAdapter(private var entries: MutableList<SatEntry>) :
    RecyclerView.Adapter<SatEntryAdapter.SatEntryHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatEntryHolder {
        val binding = ItemSatEntryBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return SatEntryHolder(binding.root, binding)
    }

    override fun onBindViewHolder(holder: SatEntryHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    fun setEntries(list: MutableList<SatEntry>) {
        entries = list
        notifyDataSetChanged()
    }

    fun getEntries(): MutableList<SatEntry> {
        return entries
    }

    inner class SatEntryHolder(itemView: View, private val binding: ItemSatEntryBinding) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(satEntry: SatEntry) {
            binding.satEntryCheckbox.text = satEntry.name
            binding.satEntryCheckbox.isChecked = satEntry.isSelected
            itemView.setOnClickListener {
                satEntry.isSelected = binding.satEntryCheckbox.isChecked
            }
        }
    }
}
