/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2022 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.presentation.entriesScreen

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.presentation.clickWithDebounce
import com.rtbishop.look4sat.presentation.getNavResult
import com.rtbishop.look4sat.presentation.setNavResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EntriesFragment : Fragment(R.layout.fragment_entries) {

    private val viewModel: EntriesViewModel by viewModels()
    private lateinit var binding: FragmentEntriesBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val entriesAdapter = EntriesAdapter(viewModel)
        binding = FragmentEntriesBinding.bind(view).apply {
            entriesRecycler.apply {
                setHasFixedSize(true)
                adapter = entriesAdapter
                layoutManager = GridLayoutManager(requireContext(), 2)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(DividerItemDecoration(requireContext(), 1))
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (dy > 0 && entriesFab.visibility == View.VISIBLE) entriesFab.hide()
                        else if (dy < 0 && entriesFab.visibility != View.VISIBLE) entriesFab.show()
                    }
                })
            }
            entriesBtnBack.clickWithDebounce { findNavController().navigateUp() }
            entriesSearch.doOnTextChanged { text, _, _, _ -> viewModel.setQuery(text.toString()) }
            entriesBtnModes.clickWithDebounce {
                val direction = EntriesFragmentDirections.entriesToModes()
                findNavController().navigate(direction)
            }
            entriesBtnSelect.clickWithDebounce { viewModel.selectCurrentItems(true) }
            entriesBtnClear.clickWithDebounce { viewModel.selectCurrentItems(false) }
            val typeMessageFormat = requireContext().getString(R.string.types_message)
            val type = if (viewModel.getSatType().isNullOrBlank()) "All" else viewModel.getSatType()
            entriesTypeMessage.text = String.format(typeMessageFormat, type)
            entriesTypeCard.setOnClickListener { showSelectTypeDialog() }
        }
        viewModel.satData.observe(viewLifecycleOwner) { satData ->
            handleSatData(satData, entriesAdapter)
            binding.entriesFab.clickWithDebounce {
                setNavResult("selection", viewModel.saveSelection())
                findNavController().popBackStack()
            }
        }
        getNavResult<List<String>>(R.id.nav_entries, "modes") { modes ->
            viewModel.saveSelectedModes(modes)
        }
    }

    private fun handleSatData(dataState: DataState<List<SatItem>>, entriesAdapter: EntriesAdapter) {
        when (dataState) {
            is DataState.Success -> {
                entriesAdapter.submitList(dataState.data)
                binding.entriesProgress.visibility = View.INVISIBLE
            }
            is DataState.Loading -> {
                binding.entriesProgress.visibility = View.VISIBLE
            }
            else -> {}
        }
    }

    private fun showSelectTypeDialog() {
        val satelliteTypes = viewModel.satTypes.toTypedArray()
        val selectedValue = satelliteTypes.indexOf(viewModel.getSatType())
        val typeFormat = requireContext().getString(R.string.types_message)
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.types_title)
            setSingleChoiceItems(satelliteTypes, selectedValue) { dialog, index ->
                val selectedItem = satelliteTypes[index]
                binding.entriesTypeMessage.text = String.format(typeFormat, selectedItem)
                viewModel.setSatType(selectedItem)
                dialog.dismiss()
            }
            create()
            show()
        }
    }
}
