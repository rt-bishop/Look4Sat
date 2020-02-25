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
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.repo.SatEntry

internal class SatEntryAdapter(private var entriesList: MutableList<SatEntry>) :
    RecyclerView.Adapter<SatEntryAdapter.SatEntryHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatEntryHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.sat_entry_item, parent, false)
        return SatEntryHolder(view)
    }

    override fun onBindViewHolder(holder: SatEntryHolder, position: Int) {
        holder.bind(entriesList[position])
    }

    override fun getItemCount(): Int {
        return entriesList.size
    }

    fun setEntries(entries: MutableList<SatEntry>) {
        entriesList = entries
        notifyDataSetChanged()
    }

    internal inner class SatEntryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val satEntryItem: CheckBox = itemView.findViewById(R.id.sat_entry_item)

        fun bind(satEntry: SatEntry) {
            satEntryItem.text = satEntry.name
            satEntryItem.isChecked = satEntry.isSelected
            itemView.setOnClickListener { satEntry.isSelected = satEntryItem.isChecked }
        }
    }
}
