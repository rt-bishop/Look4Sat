package com.rtbishop.look4sat.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.databinding.ItemTleSourceBinding

class TleSourcesAdapter(private var sources: MutableList<TleSource>) :
    RecyclerView.Adapter<TleSourcesAdapter.TleSourceHolder>() {

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

    fun setSources(sources: MutableList<TleSource>) {
        this.sources = sources
    }

    fun getSources(): MutableList<TleSource> {
        return sources
    }

    inner class TleSourceHolder(itemView: View, private val binding: ItemTleSourceBinding) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(source: TleSource) {
            binding.tleSourceBtnDel.setOnClickListener {
                sources.remove(source)
                notifyItemRemoved(adapterPosition)
            }
            binding.tleSourceUrl.addTextChangedListener {
                source.url = it.toString()
                notifyItemChanged(adapterPosition)
            }
        }
    }
}