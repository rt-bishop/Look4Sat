/*******************************************************************************
 Look4Sat. Amateur radio satellite tracker and pass predictor.
 Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/

package com.rtbishop.look4sat.ui.mainScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.SharedViewModel
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.databinding.DialogSourcesBinding
import com.rtbishop.look4sat.ui.adapters.SourcesAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SourcesDialog : AppCompatDialogFragment() {

    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_sources, group, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        viewModel.getSources().observe(viewLifecycleOwner, { sources ->
            val sourcesAdapter = SourcesAdapter(sources as MutableList<TleSource>)
            DialogSourcesBinding.bind(view).apply {
                tleSourcesRecycler.apply {
                    adapter = sourcesAdapter
                    layoutManager = LinearLayoutManager(requireContext())
                }
                tleSourceBtnAdd.setOnClickListener {
                    sourcesAdapter.addSource()
                }
                tleSourcesBtnPos.setOnClickListener {
                    viewModel.updateEntriesFromSources(sourcesAdapter.getSources())
                    dismiss()
                }
                tleSourcesBtnNeg.setOnClickListener { dismiss() }
            }
        })
    }
}