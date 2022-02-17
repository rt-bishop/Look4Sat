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
package com.rtbishop.look4sat.presentation.settingsScreen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.databinding.ItemSourceBinding
import com.rtbishop.look4sat.framework.model.DataSource

class SourcesAdapter(private val clickListener: SourcesClickListener) :
    RecyclerView.Adapter<SourcesAdapter.SourceHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<DataSource>() {
        override fun areItemsTheSame(oldItem: DataSource, newItem: DataSource): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: DataSource, newItem: DataSource): Boolean {
            return oldItem.url == newItem.url
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)
    private val httpsString = "https://"

    fun getSources() = differ.currentList.filter { source -> source.url.contains(httpsString) }

    fun addSource() {
        submitList((getSources() as MutableList).apply { add(DataSource(httpsString)) })
    }

    fun removeSource(source: DataSource) {
        submitList((getSources() as MutableList).apply { remove(source) })
    }

    fun submitList(items: List<DataSource>) = differ.submitList(items)

    override fun getItemCount(): Int = differ.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceHolder {
        return SourceHolder.from(parent)
    }

    override fun onBindViewHolder(holder: SourceHolder, position: Int) {
        holder.bind(differ.currentList[position], clickListener)
    }

    class SourceHolder(private val binding: ItemSourceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(source: DataSource, listener: SourcesClickListener) {
            binding.run {
                sourceText.setText(source.url)
                sourceText.doOnTextChanged { text, _, _, _ -> source.url = text.toString() }
                sourceInput.setEndIconOnClickListener { listener.removeSource(source) }
            }
        }

        companion object {
            fun from(parent: ViewGroup): SourceHolder {
                val inflater = LayoutInflater.from(parent.context)
                return SourceHolder(ItemSourceBinding.inflate(inflater, parent, false))
            }
        }
    }

    interface SourcesClickListener {
        fun removeSource(source: DataSource)
    }
}
