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

package com.rtbishop.look4sat.ui

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.SearchView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtbishop.look4sat.databinding.DialogSatEntryBinding
import com.rtbishop.look4sat.repo.SatEntry
import com.rtbishop.look4sat.ui.adapters.SatEntryAdapter
import java.util.*

class SatEntryDialog : AppCompatDialogFragment(), SearchView.OnQueryTextListener {

    private lateinit var satEntryAdapter: SatEntryAdapter
    private lateinit var entriesListener: EntriesSubmitListener

    private var _binding: DialogSatEntryBinding? = null
    private val binding get() = _binding!!

    private var entriesList = mutableListOf<SatEntry>()
    private var selectionList = mutableListOf<Int>()
    private var selectAllToggle = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSatEntryBinding.inflate(requireActivity().layoutInflater)
        val satEntryDialog = Dialog(requireActivity()).apply {
            window?.requestFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        entriesList = checkSelectedEntries(entriesList, selectionList)
        satEntryAdapter = SatEntryAdapter(entriesList)

        binding.dialogSearch.apply {
            setOnQueryTextListener(this@SatEntryDialog)
            onActionViewExpanded()
            clearFocus()
        }

        binding.dialogRecycler.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = satEntryAdapter
        }

        binding.dialogBtnPositive.setOnClickListener { onPositiveClicked() }
        binding.dialogBtnNegative.setOnClickListener { onNegativeClicked() }
        binding.dialogBtnNeutral.setOnClickListener { onNeutralClicked() }

        return satEntryDialog
    }

    fun setEntriesList(list: MutableList<SatEntry>): SatEntryDialog {
        entriesList = list
        return this
    }

    fun setSelectionList(list: MutableList<Int>): SatEntryDialog {
        selectAllToggle = list.isEmpty()
        selectionList = list
        return this
    }

    fun setEntriesListener(listener: EntriesSubmitListener): SatEntryDialog {
        entriesListener = listener
        return this
    }

    private fun checkSelectedEntries(entries: MutableList<SatEntry>, selection: MutableList<Int>)
            : MutableList<SatEntry> {
        selection.forEach { entries[it].isSelected = true }
        return entries
    }

    private fun filterEntries(list: MutableList<SatEntry>, query: String): MutableList<SatEntry> {
        val searchQuery = query.toLowerCase(Locale.getDefault())
        if ((searchQuery == "") or searchQuery.isEmpty()) return list

        val filteredList = mutableListOf<SatEntry>()
        list.forEach {
            val name = it.name.toLowerCase(Locale.getDefault())
            if (name.contains(searchQuery)) filteredList.add(it)
        }
        return filteredList
    }

    private fun onPositiveClicked() {
        val listToSubmit = mutableListOf<Int>().apply {
            entriesList.forEach { if (it.isSelected) this.add(it.id) }
        }
        entriesListener.onEntriesSubmit(listToSubmit)
        dismiss()
    }

    private fun onNegativeClicked() {
        dismiss()
    }

    private fun onNeutralClicked() {
        selectAllToggle = if (selectAllToggle) {
            entriesList.forEach { it.isSelected = true }
            satEntryAdapter.setEntries(entriesList)
            false
        } else {
            entriesList.forEach { it.isSelected = false }
            satEntryAdapter.setEntries(entriesList)
            true
        }
    }

    override fun onQueryTextChange(newText: String): Boolean {
        entriesList = checkSelectedEntries(entriesList, selectionList)
        val filteredList = filterEntries(entriesList, newText)
        satEntryAdapter.setEntries(filteredList)
        return false
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    interface EntriesSubmitListener {
        fun onEntriesSubmit(list: MutableList<Int>)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
