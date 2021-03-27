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
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.data.model.SatItem
import com.rtbishop.look4sat.databinding.ItemSatEntryBinding
import java.util.*

class EntriesAdapter : RecyclerView.Adapter<EntriesAdapter.SatItemHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<SatItem>() {
        override fun areItemsTheSame(oldItem: SatItem, newItem: SatItem): Boolean {
            return oldItem.catNum == newItem.catNum
        }

        override fun areContentsTheSame(oldItem: SatItem, newItem: SatItem): Boolean {
            return oldItem.isSelected != newItem.isSelected
        }
    }
    private val listDiffer = AsyncListDiffer(this, diffCallback)
    private val allItems = mutableListOf<SatItem>()
    private var shouldSelectAll = true

    fun getSelectedIds(): List<Int> {
        return allItems.filter { it.isSelected }.map { it.catNum }
    }

    fun submitAllItems(items: List<SatItem>) {
        allItems.clear()
        allItems.addAll(items)
        listDiffer.submitList(items)
    }

    fun selectCurrentItems() {
        val newList = mutableListOf<SatItem>()
        listDiffer.currentList.forEach { item ->
            item.isSelected = shouldSelectAll
            newList.add(item)
        }
        submitCurrentItems(newList)
        shouldSelectAll = !shouldSelectAll
    }

    fun filterItems(query: String) {
        if (query.isEmpty()) {
            submitCurrentItems(allItems)
        } else {
            try {
                filterByCatNum(query.toInt())
            } catch (e: NumberFormatException) {
                filterByName(query)
            }
        }
        shouldSelectAll = true
    }

    private fun submitCurrentItems(items: List<SatItem>) {
        listDiffer.submitList(items)
    }

    private fun filterByCatNum(catNum: Int) {
        submitCurrentItems(allItems.filter { it.catNum == catNum })
    }

    private fun filterByName(name: String) {
        val satName = name.toLowerCase(Locale.getDefault())
        val filteredItems = allItems.filter { item ->
            val itemName = item.name.toLowerCase(Locale.getDefault())
            itemName.contains(satName)
        }
        submitCurrentItems(filteredItems)
    }

    override fun getItemCount(): Int = listDiffer.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatItemHolder {
        return SatItemHolder.from(parent)
    }

    override fun onBindViewHolder(holder: SatItemHolder, position: Int) {
        holder.bind(listDiffer.currentList[position])
    }

    class SatItemHolder private constructor(private val binding: ItemSatEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SatItem) {
            binding.satItemCheckbox.text = item.name
            binding.satItemCheckbox.isChecked = item.isSelected
            itemView.setOnClickListener { item.isSelected = item.isSelected.not() }
        }

        companion object {
            fun from(parent: ViewGroup): SatItemHolder {
                val inflater = LayoutInflater.from(parent.context)
                return SatItemHolder(ItemSatEntryBinding.inflate(inflater, parent, false))
            }
        }
    }
}
