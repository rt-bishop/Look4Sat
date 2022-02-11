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
                val hoursAhead = try {
                    hoursAheadEdit.text.toString().toInt()
                } catch (exception: Exception) {
                    8
                }
                val minElevation = try {
                    minElevEdit.text.toString().toDouble()
                } catch (exception: Exception) {
                    16.0
                }
                setNavResult("prefs", Pair(hoursAhead, minElevation))
                dismiss()
            }
            passPrefBtnNeg.setOnClickListener { dismiss() }
        }
    }
}
