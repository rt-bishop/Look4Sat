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

package com.rtbishop.look4sat.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.SearchView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.databinding.DialogSatEntryBinding
import com.rtbishop.look4sat.ui.adapters.SatEntryAdapter
import java.util.*

class SatEntryDialogFragment(private var entries: MutableList<SatEntry>) :
    AppCompatDialogFragment(), SearchView.OnQueryTextListener {

    private lateinit var entriesListener: EntriesSubmitListener
    private val entryAdapter = SatEntryAdapter(entries)
    private var selectAllToggle = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogSatEntryBinding.inflate(requireActivity().layoutInflater)
        val satEntryDialog = Dialog(requireActivity()).apply {
            window?.requestFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        binding.dialogSearch.apply {
            setOnQueryTextListener(this@SatEntryDialogFragment)
            onActionViewExpanded()
            clearFocus()
        }

        binding.dialogRecycler.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = entryAdapter
        }

        binding.dialogBtnPositive.setOnClickListener { onPositiveClicked() }
        binding.dialogBtnNegative.setOnClickListener { onNegativeClicked() }
        binding.dialogBtnNeutral.setOnClickListener { onNeutralClicked() }

        return satEntryDialog
    }

    fun setEntriesListener(listener: EntriesSubmitListener): SatEntryDialogFragment {
        entriesListener = listener
        return this
    }

    private fun filterEntries(list: MutableList<SatEntry>, query: String): MutableList<SatEntry> {
        if (query.isEmpty()) return list
        return try {
            filterByCatNum(list, query.toInt())
        } catch (e: NumberFormatException) {
            filterByName(list, query)
        }
    }

    private fun filterByCatNum(list: MutableList<SatEntry>, catNum: Int): MutableList<SatEntry> {
        val filteredList = list.filter { it.catNum == catNum }
        return filteredList as MutableList<SatEntry>
    }

    private fun filterByName(list: MutableList<SatEntry>, query: String): MutableList<SatEntry> {
        val searchQuery = query.toLowerCase(Locale.getDefault())
        val filteredList = mutableListOf<SatEntry>()
        list.forEach {
            val name = it.name.toLowerCase(Locale.getDefault())
            if (name.contains(searchQuery)) filteredList.add(it)
        }
        return filteredList
    }

    private fun onPositiveClicked() {
        val catNumList = mutableListOf<Int>().apply {
            entries.forEach { if (it.isSelected) this.add(it.catNum) }
        }
        entriesListener.onEntriesSubmit(catNumList)
        dismiss()
    }

    private fun onNegativeClicked() {
        dismiss()
    }

    private fun onNeutralClicked() {
        selectAllToggle = if (selectAllToggle) {
            val currentList = entryAdapter.getEntries()
            currentList.forEach { it.isSelected = true }
            entryAdapter.setEntries(currentList)
            false
        } else {
            val currentList = entryAdapter.getEntries()
            currentList.forEach { it.isSelected = false }
            entryAdapter.setEntries(currentList)
            true
        }
    }

    override fun onQueryTextChange(newText: String): Boolean {
        val filteredList = filterEntries(entries, newText)
        entryAdapter.setEntries(filteredList)
        return false
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    interface EntriesSubmitListener {
        fun onEntriesSubmit(catNumList: MutableList<Int>)
    }
}
