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
package com.rtbishop.look4sat.ui.entriesScreen

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.data.model.SatItem
import com.rtbishop.look4sat.databinding.ItemSatEntryBinding
import java.util.*

class EntriesAdapter : RecyclerView.Adapter<EntriesAdapter.SatItemHolder>(),
    SearchView.OnQueryTextListener {
    
    private val allItems = mutableListOf<SatItem>()
    private val currentItems = mutableListOf<SatItem>()
    private var shouldSearchAll = true
    
    fun getItems(): List<SatItem> {
        return allItems
    }
    
    fun setItems(items: List<SatItem>) {
        allItems.clear()
        allItems.addAll(items)
        currentItems.clear()
        currentItems.addAll(items)
        notifyDataSetChanged()
    }
    
    fun selectAllItems() {
        currentItems.forEach { it.isSelected = shouldSearchAll }
        shouldSearchAll = shouldSearchAll.not()
        notifyDataSetChanged()
    }
    
    override fun onQueryTextChange(newText: String): Boolean {
        currentItems.clear()
        currentItems.addAll(filterItems(allItems, newText).toMutableList())
        notifyDataSetChanged()
        return false
    }
    
    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }
    
    private fun filterItems(list: List<SatItem>, query: String): List<SatItem> {
        shouldSearchAll = true
        if (query.isEmpty()) return list
        return try {
            filterByCatNum(list, query.toInt())
        } catch (e: NumberFormatException) {
            filterByName(list, query)
        }
    }
    
    private fun filterByCatNum(list: List<SatItem>, catNum: Int): List<SatItem> {
        return list.filter { it.catNum == catNum }
    }

    private fun filterByName(list: List<SatItem>, query: String): List<SatItem> {
        val defaultLocale = Locale.getDefault()
        val searchQuery = query.toLowerCase(defaultLocale)
        return list.filter { satItem ->
            val lowerCaseItem = satItem.name.toLowerCase(defaultLocale)
            lowerCaseItem.contains(searchQuery)
        }
    }

    override fun getItemCount(): Int = currentItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatItemHolder {
        return SatItemHolder.from(parent)
    }

    override fun onBindViewHolder(holder: SatItemHolder, position: Int) {
        holder.bind(currentItems[position])
    }

    class SatItemHolder private constructor(private val binding: ItemSatEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SatItem) {
            binding.satEntryCheckbox.text = item.name
            binding.satEntryCheckbox.isChecked = item.isSelected
            itemView.setOnClickListener {
                item.isSelected = binding.satEntryCheckbox.isChecked
            }
        }

        companion object {
            fun from(parent: ViewGroup): SatItemHolder {
                val inflater = LayoutInflater.from(parent.context)
                return SatItemHolder(ItemSatEntryBinding.inflate(inflater, parent, false))
            }
        }
    }
}
