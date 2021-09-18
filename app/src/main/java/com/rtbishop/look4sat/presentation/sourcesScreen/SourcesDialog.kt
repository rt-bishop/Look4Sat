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
package com.rtbishop.look4sat.presentation.sourcesScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.PreferencesSource
import com.rtbishop.look4sat.databinding.DialogSourcesBinding
import com.rtbishop.look4sat.utility.setNavResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SourcesDialog : AppCompatDialogFragment() {

    @Inject
    lateinit var prefsManager: PreferencesSource

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_sources, group, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        val sources = prefsManager.loadTleSources().map { DataSource(it) }
        val sourcesAdapter = SourcesAdapter().apply { setSources(sources) }
        DialogSourcesBinding.bind(view).apply {
            dialog?.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            sourcesRecycler.apply {
                adapter = sourcesAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }
            sourcesBtnAdd.setOnClickListener {
                sourcesAdapter.addSource()
            }
            sourcesBtnPos.setOnClickListener {
                setNavResult("sources", sourcesAdapter.getSources().map { it.url })
                dismiss()
            }
            sourcesBtnNeg.setOnClickListener { dismiss() }
        }
    }
}
