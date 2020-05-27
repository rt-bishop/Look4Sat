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
import android.view.Window
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.databinding.DialogTleSourcesBinding
import com.rtbishop.look4sat.ui.adapters.TleSourcesAdapter

class TleSourcesDialogFragment(sources: List<TleSource>) :
    AppCompatDialogFragment() {

    private lateinit var sourcesListener: SourcesSubmitListener
    private val emptySource = TleSource("")
    private val sourcesAdapter = TleSourcesAdapter(sources.toMutableList())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogTleSourcesBinding.inflate(requireActivity().layoutInflater)
        val tleSourcesDialog = Dialog(requireActivity()).apply {
            window?.requestFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        binding.tleSourcesRecycler.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = sourcesAdapter
        }

        binding.tleSourceBtnAdd.setOnClickListener {
            val tempSources = sourcesAdapter.getSources()
            tempSources.add(emptySource)
            sourcesAdapter.setSources(tempSources)
            sourcesAdapter.notifyDataSetChanged()
        }

        binding.tleSourcesBtnNeg.setOnClickListener { dismiss() }
        binding.tleSourcesBtnPos.setOnClickListener {
            val filteredSources = sourcesAdapter.getSources()
                .filter { it.url != String() && it.url != " " && it.url.contains("https://") }
            sourcesListener.onSourcesSubmit(filteredSources)
            dismiss()
        }

        return tleSourcesDialog
    }

    fun setSourcesListener(listener: SourcesSubmitListener): TleSourcesDialogFragment {
        sourcesListener = listener
        return this
    }

    interface SourcesSubmitListener {
        fun onSourcesSubmit(list: List<TleSource>)
    }
}