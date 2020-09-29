package com.rtbishop.look4sat.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
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

class EntriesFragment : Fragment(R.layout.fragment_entries) {

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
            }
            importWeb.setOnClickListener { showImportFromWebDialog() }
            importFile.setOnClickListener { showImportFromFileDialog() }
            searchBar.setOnQueryTextListener(entriesAdapter)
            selectAll.setOnClickListener { entriesAdapter.selectAll() }
            entriesFab.setOnClickListener { goToPassesAndCalculateForSelection() }
        }
    }

    private fun setupObservers() {
        viewModel.tleSources.observe(viewLifecycleOwner, { tleSources = it })
        viewModel.allEntries.observe(viewLifecycleOwner, {
            entriesAdapter.setEntries(it as MutableList<SatEntry>)
        })
    }

    private fun goToPassesAndCalculateForSelection() {
        val catNumList = mutableListOf<Int>()
        entriesAdapter.getEntries().forEach { if (it.isSelected) catNumList.add(it.catNum) }
        viewModel.updateEntriesSelection(catNumList)
        requireView().findNavController().navigate(R.id.action_entries_to_passes)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == pickFileReqCode && resultCode == AppCompatActivity.RESULT_OK) {
            data?.data?.also { uri -> viewModel.updateEntriesFromFile(uri) }
        } else super.onActivityResult(requestCode, resultCode, data)
    }
}