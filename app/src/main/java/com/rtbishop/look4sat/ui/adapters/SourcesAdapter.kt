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
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.databinding.ItemTleSourceBinding

class SourcesAdapter(private var sources: MutableList<TleSource>) :
    RecyclerView.Adapter<SourcesAdapter.TleSourceHolder>() {

    fun getSources(): MutableList<TleSource> {
        return sources
    }

    fun setSources(list: MutableList<TleSource>) {
        sources = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TleSourceHolder {
        val binding = ItemTleSourceBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return TleSourceHolder(binding.root, binding)
    }

    override fun onBindViewHolder(holder: TleSourceHolder, position: Int) {
        holder.bind(sources[position])
    }

    override fun getItemCount(): Int {
        return sources.size
    }

    inner class TleSourceHolder(itemView: View, val binding: ItemTleSourceBinding) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(source: TleSource) {
            binding.tleSourceUrl.setText(source.url)
            binding.tleSourceUrl.doOnTextChanged { text, _, _, _ -> source.url = text.toString() }
            binding.tleSourceInputLayout.setEndIconOnClickListener {
                sources.remove(source)
                notifyItemRemoved(adapterPosition)
            }
        }
    }
}