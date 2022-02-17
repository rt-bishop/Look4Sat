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
package com.rtbishop.look4sat.presentation.settingsScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.DialogSourcesBinding
import com.rtbishop.look4sat.domain.IDataRepository
import com.rtbishop.look4sat.framework.model.DataSource
import com.rtbishop.look4sat.presentation.setNavResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SourcesDialog : AppCompatDialogFragment(), SourcesAdapter.SourcesClickListener {

    @Inject
    lateinit var repository: IDataRepository
    private lateinit var binding: DialogSourcesBinding
    private lateinit var sourcesAdapter: SourcesAdapter

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View {
        binding = DialogSourcesBinding.inflate(inflater, group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        lifecycleScope.launchWhenResumed {
            val sources = repository.getDataSources().map { url -> DataSource(url) }
            val layoutManager = LinearLayoutManager(requireContext())
            sourcesAdapter = SourcesAdapter(this@SourcesDialog).apply { submitList(sources) }
            binding.run {
                sourcesRecycler.apply {
                    this.adapter = sourcesAdapter
                    this.layoutManager = layoutManager
                }
                sourcesBtnAdd.setOnClickListener { sourcesAdapter.addSource() }
                sourcesBtnNeg.setOnClickListener { dismiss() }
                sourcesBtnPos.setOnClickListener {
                    setNavResult("sources", sourcesAdapter.getSources().map { item -> item.url })
                    dismiss()
                }
            }
        }
    }

    override fun removeSource(source: DataSource) = sourcesAdapter.removeSource(source)
}
