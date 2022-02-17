package com.rtbishop.look4sat.presentation.entriesScreen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.databinding.ItemModeBinding

class ModesAdapter(private val clickListener: ModesClickListener) :
    RecyclerView.Adapter<ModesAdapter.ModeHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)
    private var selectedModes = listOf<String>()

    fun submitModes(items: List<String>, selected: List<String>) {
        differ.submitList(items)
        selectedModes = selected
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModeHolder {
        return ModeHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ModeHolder, position: Int) {
        val currentItem = differ.currentList[position]
        holder.bind(currentItem, selectedModes.contains(currentItem), clickListener)
    }

    class ModeHolder(private val binding: ItemModeBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mode: String, isSelected: Boolean, listener: ModesClickListener) {
            binding.modeCheckbox.text = mode
            binding.modeCheckbox.isChecked = isSelected
            binding.modeCheckbox.setOnClickListener {
                listener.onModeClicked(mode, isSelected.not())
            }
        }

        companion object {
            fun from(parent: ViewGroup): ModeHolder {
                val inflater = LayoutInflater.from(parent.context)
                return ModeHolder(ItemModeBinding.inflate(inflater, parent, false))
            }
        }
    }

    interface ModesClickListener {
        fun onModeClicked(mode: String, isSelected: Boolean)
    }
}
