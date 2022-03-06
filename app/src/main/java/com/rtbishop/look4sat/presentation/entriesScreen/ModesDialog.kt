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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.DialogModesBinding
import com.rtbishop.look4sat.domain.ISettingsManager
import com.rtbishop.look4sat.presentation.setNavResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ModesDialog : AppCompatDialogFragment(), ModesAdapter.ModesClickListener {

    @Inject
    lateinit var preferences: ISettingsManager
    private lateinit var binding: DialogModesBinding
    private val allModes = listOf(
        "AFSK", "AFSK S-Net", "AFSK SALSAT", "AHRPT", "AM", "APT", "BPSK", "BPSK PMT-A3",
        "CERTO", "CW", "DQPSK", "DSTAR", "DUV", "FFSK", "FM", "FMN", "FSK", "FSK AX.100 Mode 5",
        "FSK AX.100 Mode 6", "FSK AX.25 G3RUH", "GFSK", "GFSK Rktr", "GMSK", "HRPT", "LoRa",
        "LRPT", "LSB", "MFSK", "MSK", "MSK AX.100 Mode 5", "MSK AX.100 Mode 6", "OFDM", "OQPSK",
        "PSK", "PSK31", "PSK63", "QPSK", "QPSK31", "QPSK63", "SSTV", "USB", "WSJT"
    )
    private val selectedModes = mutableListOf<String>()

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View {
        binding = DialogModesBinding.inflate(inflater, group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            (resources.displayMetrics.heightPixels * 0.76).toInt()
        )
        val modesAdapter = ModesAdapter(this@ModesDialog)
        val layoutManager = GridLayoutManager(context, 2)
        val itemDecoration = DividerItemDecoration(context, layoutManager.orientation)
        selectedModes.addAll(preferences.loadModesSelection())
        modesAdapter.submitModes(allModes, selectedModes)
        binding.run {
            modesRecycler.apply {
                setHasFixedSize(true)
                this.adapter = modesAdapter
                this.layoutManager = layoutManager
                addItemDecoration(itemDecoration)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            }
            modesBtnNeg.setOnClickListener { dismiss() }
            modesBtnPos.setOnClickListener {
                setNavResult("modes", selectedModes.toList())
                dismiss()
            }
        }
    }

    override fun onModeClicked(mode: String, isSelected: Boolean) {
        when {
            isSelected -> selectedModes.add(mode)
            selectedModes.contains(mode) -> selectedModes.remove(mode)
        }
    }
}
