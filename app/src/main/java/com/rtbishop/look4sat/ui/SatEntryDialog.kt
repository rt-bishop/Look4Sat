package com.rtbishop.look4sat.ui

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.repo.SatEntry
import com.rtbishop.look4sat.ui.adapters.SatEntryAdapter
import java.util.*

class SatEntryDialog : AppCompatDialogFragment(), SearchView.OnQueryTextListener {

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var dialogBtnPositive: TextView
    private lateinit var dialogBtnNegative: TextView
    private lateinit var dialogBtnNeutral: TextView
    private lateinit var satEntryAdapter: SatEntryAdapter
    private lateinit var entriesListener: EntriesSubmitListener

    private var entriesList = mutableListOf<SatEntry>()
    private var selectionList = mutableListOf<Int>()
    private var selectAllToggle = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val satEntryDialog = Dialog(requireActivity()).apply {
            window?.requestFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_sat_entry)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        entriesList = checkSelectedEntries(entriesList, selectionList)
        satEntryAdapter = SatEntryAdapter(entriesList)

        findViews(satEntryDialog)
        setListeners()

        searchView.apply {
            setOnQueryTextListener(this@SatEntryDialog)
            onActionViewExpanded()
            clearFocus()
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = satEntryAdapter
        }

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

    private fun findViews(dialog: Dialog) {
        searchView = dialog.findViewById(R.id.dialog_search)
        recyclerView = dialog.findViewById(R.id.dialog_recycler)
        dialogBtnPositive = dialog.findViewById(R.id.dialog_btn_positive)
        dialogBtnNegative = dialog.findViewById(R.id.dialog_btn_negative)
        dialogBtnNeutral = dialog.findViewById(R.id.dialog_btn_neutral)
    }

    private fun setListeners() {
        dialogBtnPositive.setOnClickListener { onPositiveClicked() }
        dialogBtnNegative.setOnClickListener { onNegativeClicked() }
        dialogBtnNeutral.setOnClickListener { onNeutralClicked() }
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
}