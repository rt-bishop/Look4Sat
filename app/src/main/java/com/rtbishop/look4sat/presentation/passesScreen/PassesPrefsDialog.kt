package com.rtbishop.look4sat.presentation.passesScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.ISettingsHandler
import com.rtbishop.look4sat.databinding.DialogPassesBinding
import com.rtbishop.look4sat.presentation.setNavResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PassesPrefsDialog : AppCompatDialogFragment() {

    @Inject
    lateinit var preferences: ISettingsHandler

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_passes, group, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        DialogPassesBinding.bind(view).apply {
            dialog?.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            hoursAheadEdit.setText(preferences.getHoursAhead().toString())
            minElevEdit.setText(preferences.getMinElevation().toString())
            passPrefBtnPos.setOnClickListener {
                val hoursAheadText = hoursAheadEdit.text.toString()
                val hoursAhead = if (hoursAheadText.isNotEmpty()) hoursAheadText.toInt() else 8
                val minElevText = minElevEdit.text.toString()
                val minElev = if (minElevText.isNotEmpty()) minElevText.toDouble() else 16.0
                setNavResult("prefs", Pair(hoursAhead, minElev))
                dismiss()
            }
            passPrefBtnNeg.setOnClickListener { dismiss() }
        }
    }
}
