/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.ui.sourcesDialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.DialogSourcesBinding
import com.rtbishop.look4sat.ui.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SourcesDialog : AppCompatDialogFragment() {

    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_sources, group, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        val sourcesAdapter = SourcesAdapter().apply { setSources(viewModel.getSources()) }
        DialogSourcesBinding.bind(view).apply {
            dialog?.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
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
    }
}