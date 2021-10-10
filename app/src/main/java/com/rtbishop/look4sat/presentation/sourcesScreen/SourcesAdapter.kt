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
package com.rtbishop.look4sat.presentation.sourcesScreen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.databinding.ItemSourceBinding
import com.rtbishop.look4sat.framework.model.DataSource

class SourcesAdapter(private val sources: MutableList<DataSource> = mutableListOf()) :
    RecyclerView.Adapter<SourcesAdapter.TleSourceHolder>() {

    fun getSources(): List<DataSource> {
        return sources.filter { it.sourceUrl.contains("https://") }
    }

    fun setSources(list: List<DataSource>) {
        sources.clear()
        sources.addAll(list)
    }

    fun addSource() {
        sources.add(DataSource())
        notifyItemInserted(itemCount - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TleSourceHolder {
        val binding = ItemSourceBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return TleSourceHolder(binding)
    }

    override fun onBindViewHolder(holder: TleSourceHolder, position: Int) {
        holder.bind(sources[position])
    }

    override fun getItemCount(): Int {
        return sources.size
    }

    inner class TleSourceHolder(private val binding: ItemSourceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(source: DataSource) {
            binding.sourceUrl.setText(source.sourceUrl)
            binding.sourceUrl.doOnTextChanged { text, _, _, _ -> source.sourceUrl = text.toString() }
            binding.sourceInput.setEndIconOnClickListener {
                sources.remove(source)
                notifyItemRemoved(adapterPosition)
            }
        }
    }
}
