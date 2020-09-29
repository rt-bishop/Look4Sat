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
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.databinding.DialogSourcesBinding
import com.rtbishop.look4sat.ui.SharedViewModel
import com.rtbishop.look4sat.ui.adapters.SourcesAdapter

class SourcesDialog(sources: List<TleSource>, private val viewModel: SharedViewModel) :
    AppCompatDialogFragment() {

    private val sourcesAdapter = SourcesAdapter(sources as MutableList<TleSource>)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogSourcesBinding.inflate(requireActivity().layoutInflater).apply {
            tleSourcesRecycler.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = sourcesAdapter
            }
            tleSourceBtnAdd.setOnClickListener {
                val tempSources = sourcesAdapter.getSources()
                tempSources.add(TleSource(String()))
                sourcesAdapter.setSources(tempSources)
            }
            tleSourcesBtnPos.setOnClickListener {
                val filteredSources = sourcesAdapter.getSources()
                    .filter { it.url != String() && it.url != " " && it.url.contains("https://") }
                viewModel.updateSatelliteData(filteredSources)
                dismiss()
            }
            tleSourcesBtnNeg.setOnClickListener { dismiss() }
        }

        return Dialog(requireActivity()).apply {
            setContentView(binding.root)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
}