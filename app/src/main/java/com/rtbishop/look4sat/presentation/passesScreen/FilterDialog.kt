package com.rtbishop.look4sat.presentation.passesScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.DialogFilterBinding
import com.rtbishop.look4sat.domain.ISettings
import com.rtbishop.look4sat.presentation.setNavResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FilterDialog : AppCompatDialogFragment() {

    @Inject
    lateinit var preferences: ISettings

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_filter, group, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        DialogFilterBinding.bind(view).apply {
            dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
            dialog?.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.94).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            filterHoursEdit.setText(preferences.getHoursAhead().toString())
            filterElevEdit.setText(preferences.getMinElevation().toString())
            filterBtnPos.setOnClickListener {
                val hoursAhead = try {
                    filterHoursEdit.text.toString().toInt()
                } catch (exception: Exception) {
                    8
                }
                val minElevation = try {
                    filterElevEdit.text.toString().toDouble()
                } catch (exception: Exception) {
                    16.0
                }
                setNavResult("prefs", Pair(hoursAhead, minElevation))
                dismiss()
            }
            filterBtnNeg.setOnClickListener { dismiss() }
        }
    }
}
