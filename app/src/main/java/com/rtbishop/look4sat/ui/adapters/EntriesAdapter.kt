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
import android.view.ViewGroup
import android.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.databinding.ItemSatEntryBinding
import java.util.*

class EntriesAdapter : RecyclerView.Adapter<EntriesAdapter.SatEntryHolder>(),
    SearchView.OnQueryTextListener {

    private var shouldSearchAll = true
    private var allEntries = mutableListOf<SatEntry>()
    private var currentEntries = mutableListOf<SatEntry>()

    fun getEntries(): MutableList<SatEntry> {
        return allEntries
    }

    fun setEntries(list: MutableList<SatEntry>) {
        allEntries = list
        currentEntries = list
        notifyDataSetChanged()
    }

    fun selectAll() {
        shouldSearchAll = if (shouldSearchAll) {
            currentEntries.forEach { it.isSelected = true }
            notifyDataSetChanged()
            false
        } else {
            currentEntries.forEach { it.isSelected = false }
            notifyDataSetChanged()
            true
        }
    }

    private fun filterEntries(list: MutableList<SatEntry>, query: String): MutableList<SatEntry> {
        if (query.isEmpty()) return list
        return try {
            filterByCatNum(list, query.toInt())
        } catch (e: NumberFormatException) {
            filterByName(list, query)
        }
    }

    private fun filterByCatNum(list: MutableList<SatEntry>, catNum: Int): MutableList<SatEntry> {
        return list.filter { it.catNum == catNum } as MutableList
    }

    private fun filterByName(list: MutableList<SatEntry>, query: String): MutableList<SatEntry> {
        val defaultLocale = Locale.getDefault()
        val filteredList = mutableListOf<SatEntry>()
        val searchQuery = query.toLowerCase(defaultLocale)
        list.forEach {
            val entryName = it.name.toLowerCase(defaultLocale)
            if (entryName.contains(searchQuery)) filteredList.add(it)
        }
        return filteredList
    }

    override fun onQueryTextChange(newText: String): Boolean {
        currentEntries = filterEntries(getEntries(), newText)
        notifyDataSetChanged()
        return false
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatEntryHolder {
        val binding = ItemSatEntryBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return SatEntryHolder(binding)
    }

    override fun onBindViewHolder(holder: SatEntryHolder, position: Int) {
        holder.bind(currentEntries[position])
    }

    override fun getItemCount(): Int {
        return currentEntries.size
    }

    inner class SatEntryHolder(private val binding: ItemSatEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(satEntry: SatEntry) {
            binding.satEntryCheckbox.text = satEntry.name
            binding.satEntryCheckbox.isChecked = satEntry.isSelected
            itemView.setOnClickListener {
                satEntry.isSelected = binding.satEntryCheckbox.isChecked
            }
        }
    }
}
