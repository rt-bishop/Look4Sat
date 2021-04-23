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
package com.rtbishop.look4sat.presentation.entriesScreen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.databinding.SatItemBinding
import com.rtbishop.look4sat.domain.model.SatItem

class EntriesAdapter : RecyclerView.Adapter<EntriesAdapter.SatItemHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<SatItem>() {
        override fun areItemsTheSame(oldItem: SatItem, newItem: SatItem): Boolean {
            return oldItem.catNum == newItem.catNum
        }

        override fun areContentsTheSame(oldItem: SatItem, newItem: SatItem): Boolean {
            return oldItem.isSelected == newItem.isSelected
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)
    private lateinit var clickListener: EntriesClickListener

    interface EntriesClickListener {
        fun updateSelection(catNums: List<Int>, isSelected: Boolean)
    }

    fun setEntriesClickListener(listener: EntriesClickListener) {
        clickListener = listener
    }

    fun submitList(items: List<SatItem>) {
        differ.submitList(items)
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatItemHolder {
        return SatItemHolder.from(parent)
    }

    override fun onBindViewHolder(holder: SatItemHolder, position: Int) {
        holder.bind(differ.currentList[position], clickListener)
    }

    class SatItemHolder private constructor(private val binding: SatItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SatItem, listener: EntriesClickListener) {
            binding.satItemCheckbox.text = item.name
            binding.satItemCheckbox.isChecked = item.isSelected
            itemView.setOnClickListener {
                listener.updateSelection(listOf(item.catNum), item.isSelected.not())
            }
        }

        companion object {
            fun from(parent: ViewGroup): SatItemHolder {
                val inflater = LayoutInflater.from(parent.context)
                return SatItemHolder(SatItemBinding.inflate(inflater, parent, false))
            }
        }
    }
}
