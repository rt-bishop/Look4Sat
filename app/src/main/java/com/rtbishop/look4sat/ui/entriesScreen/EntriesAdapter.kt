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

    private var allItems = listOf<SatItem>()
    private var selectAll = true
    private lateinit var entriesClickListener: EntriesClickListener

    fun submitAllItems(items: List<SatItem>) {
        allItems = items
        listDiffer.submitList(items)
    }

    private fun submitCurrentItems(items: List<SatItem>) {
        listDiffer.submitList(items)
    }

    fun setEntriesClickListener(listener: EntriesClickListener) {
        entriesClickListener = listener
    }

    fun filterItems(query: String) {
        selectAll = true
        if (query.isEmpty()) {
            submitCurrentItems(allItems)
        } else {
            try {
                val catNum = query.toInt()
                submitCurrentItems(filterByCatNum(allItems, catNum))
            } catch (e: NumberFormatException) {
                submitCurrentItems(filterByName(allItems, query))
            }
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

    fun selectAllItems() {
//        currentItems.forEach { it.isSelected = selectAll }
//        selectAll = selectAll.not()
//        notifyDataSetChanged()

//        val catNums = listDiffer.currentList.map { it.catNum }
//        entriesClickListener.onSelectAllClick(catNums, selectAll)
//        selectAll = selectAll.not()
    }

    interface EntriesClickListener {
        fun onItemClick(catNum: Int, isSelected: Boolean)
        fun onSelectAllClick(catNums: List<Int>, isSelected: Boolean)
    }

    private val diffCallback = object : DiffUtil.ItemCallback<SatItem>() {
        override fun areItemsTheSame(oldItem: SatItem, newItem: SatItem): Boolean {
            return oldItem.catNum == newItem.catNum
        }

        override fun areContentsTheSame(oldItem: SatItem, newItem: SatItem): Boolean {
            return oldItem.isSelected == newItem.isSelected
        }
    }

    private val listDiffer = AsyncListDiffer(this, diffCallback)

    override fun getItemCount(): Int = listDiffer.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatItemHolder {
        val inflater = LayoutInflater.from(parent.context)
        return SatItemHolder(ItemSatEntryBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: SatItemHolder, position: Int) {
        holder.bind(listDiffer.currentList[position])
    }

    inner class SatItemHolder(private val binding: ItemSatEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SatItem) {
            binding.satEntryCheckbox.text = item.name
            binding.satEntryCheckbox.isChecked = item.isSelected
            itemView.setOnClickListener {
                entriesClickListener.onItemClick(item.catNum, !item.isSelected)
            }
        }
    }
}
