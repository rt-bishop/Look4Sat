package com.rtbishop.look4sat.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.repo.SatEntry

internal class SatEntryAdapter(private var entriesList: MutableList<SatEntry>) :
    RecyclerView.Adapter<SatEntryAdapter.SatEntryHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatEntryHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.sat_entry_item, parent, false)
        return SatEntryHolder(view)
    }

    override fun onBindViewHolder(holder: SatEntryHolder, position: Int) {
        holder.bind(entriesList[position])
    }

    override fun getItemCount(): Int {
        return entriesList.size
    }

    fun setEntries(entries: MutableList<SatEntry>) {
        entriesList = entries
        notifyDataSetChanged()
    }

    internal inner class SatEntryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val satEntryItem: CheckBox = itemView.findViewById(R.id.sat_entry_item)

        fun bind(satEntry: SatEntry) {
            satEntryItem.text = satEntry.name
            satEntryItem.isChecked = satEntry.isSelected
            itemView.setOnClickListener { satEntry.isSelected = satEntryItem.isChecked }
        }
    }
}