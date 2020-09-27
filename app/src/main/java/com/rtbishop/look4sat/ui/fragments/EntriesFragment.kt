package com.rtbishop.look4sat.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtbishop.look4sat.Look4SatApp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.dagger.ViewModelFactory
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import com.rtbishop.look4sat.ui.SharedViewModel
import com.rtbishop.look4sat.ui.adapters.EntriesAdapter
import javax.inject.Inject

class EntriesFragment : Fragment(R.layout.fragment_entries),
    SourcesDialog.SourcesSubmitListener,
    SearchView.OnQueryTextListener {

    @Inject
    lateinit var factory: ViewModelFactory

    private lateinit var binding: FragmentEntriesBinding
    private lateinit var viewModel: SharedViewModel
    private val pickFileReqCode = 100
    private val entriesAdapter = EntriesAdapter()
    private var tleSources = listOf<TleSource>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEntriesBinding.bind(view)
        (requireActivity().application as Look4SatApp).appComponent.inject(this)
        viewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding.apply {
            importWeb.setOnClickListener { showSourcesDialog() }
            importFile.setOnClickListener { showFileDialog() }
            selectAll.setOnClickListener { entriesAdapter.selectAll() }
            entriesFab.setOnClickListener { goToPassesAndCalculateForSelection() }
            searchBar.setOnQueryTextListener(this@EntriesFragment)
            entriesRecycler.layoutManager = LinearLayoutManager(requireActivity())
            entriesRecycler.adapter = entriesAdapter
        }
    }

    private fun setupObservers() {
        viewModel.tleSources.observe(viewLifecycleOwner, { sources ->
            tleSources = sources
        })
        viewModel.allEntries.observe(viewLifecycleOwner, { entries ->
            entriesAdapter.setEntries(entries as MutableList<SatEntry>)
        })
    }

    // Entries update from web

    private fun showSourcesDialog() {
        val fragmentManager = childFragmentManager
        SourcesDialog(tleSources).apply {
            setSourcesListener(this@EntriesFragment)
            show(fragmentManager, "TleSourcesDialog")
        }
    }

    override fun onSourcesSubmit(list: List<TleSource>) {
        viewModel.updateSatelliteData(list)
    }

    // Entries update from file

    private fun showFileDialog() {
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            startActivityForResult(this, pickFileReqCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == pickFileReqCode && resultCode == AppCompatActivity.RESULT_OK) {
            data?.data?.also { uri -> viewModel.updateEntriesFromFile(uri) }
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    // Search and select entries

    private fun goToPassesAndCalculateForSelection() {
        val catNumList = mutableListOf<Int>().apply {
            entriesAdapter.getEntries().forEach { if (it.isSelected) this.add(it.catNum) }
        }
        viewModel.updateEntriesSelection(catNumList)
        requireView().findNavController().navigate(R.id.action_entries_to_passes)
    }

    override fun onQueryTextChange(newText: String): Boolean {
        val filteredList = entriesAdapter.filterEntries(entriesAdapter.getEntries(), newText)
        entriesAdapter.setEntries(filteredList)
        return false
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }
}