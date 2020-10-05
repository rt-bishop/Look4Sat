package com.rtbishop.look4sat.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.SharedViewModel
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import com.rtbishop.look4sat.ui.adapters.EntriesAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EntriesFragment : Fragment(R.layout.fragment_entries) {

    private lateinit var binding: FragmentEntriesBinding
    private val viewModel: SharedViewModel by activityViewModels()
    private val pickFileReqCode = 100
    private val entriesAdapter = EntriesAdapter()
    private var tleSources = listOf<TleSource>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEntriesBinding.bind(view)
        setupComponents()
        setupObservers()
    }

    private fun setupComponents() {
        binding.apply {
            entriesRecycler.apply {
                val linearLayoutMgr = LinearLayoutManager(requireContext())
                val divider = DividerItemDecoration(requireContext(), linearLayoutMgr.orientation)
                val drawable = ResourcesCompat
                    .getDrawable(resources, R.drawable.entries_divider, requireActivity().theme)
                drawable?.let { divider.setDrawable(it) }
                layoutManager = linearLayoutMgr
                adapter = entriesAdapter
                addItemDecoration(divider)
                setHasFixedSize(true)
            }
            importWeb.setOnClickListener { showImportFromWebDialog() }
            importFile.setOnClickListener { showImportFromFileDialog() }
            selectAll.setOnClickListener { entriesAdapter.selectAll() }
            entriesFab.setOnClickListener { navigateToPasses() }
            searchBar.setOnQueryTextListener(entriesAdapter)
            searchBar.clearFocus()
        }
    }

    private fun setupObservers() {
        viewModel.getSources().observe(viewLifecycleOwner, { tleSources = it })
        viewModel.getEntries().observe(viewLifecycleOwner, {
            viewModel.setSelectedEntries(it)
            entriesAdapter.setEntries(it as MutableList<SatEntry>)
        })
    }

    private fun showImportFromWebDialog() {
        SourcesDialog(tleSources, viewModel).show(childFragmentManager, "SourcesDialog")
    }

    private fun showImportFromFileDialog() {
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            startActivityForResult(this, pickFileReqCode)
        }
    }

    private fun navigateToPasses() {
        binding.searchBar.clearFocus()
        viewModel.updateEntriesSelection(entriesAdapter.getEntries())
        requireView().findNavController().navigate(R.id.action_entries_to_passes)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == pickFileReqCode && resultCode == AppCompatActivity.RESULT_OK) {
            data?.data?.also { uri -> viewModel.updateEntriesFromFile(uri) }
        } else super.onActivityResult(requestCode, resultCode, data)
    }
}