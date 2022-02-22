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
import com.rtbishop.look4sat.domain.ISettings
import com.rtbishop.look4sat.presentation.setNavResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ModesDialog : AppCompatDialogFragment(), ModesAdapter.ModesClickListener {

    @Inject
    lateinit var preferences: ISettings
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
