package com.rtbishop.look4sat.presentation.settingsScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.ISettingsHandler
import com.rtbishop.look4sat.databinding.DialogLocatorBinding
import com.rtbishop.look4sat.presentation.setNavResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocatorDialog : AppCompatDialogFragment() {

    @Inject
    lateinit var preferences: ISettingsHandler

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_locator, group, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        DialogLocatorBinding.bind(view).run {
            dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
            dialog?.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.94).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
//            locatorEdit.setText(preferences.getQthLocator())
            locatorPosBtn.setOnClickListener {
                setNavResult("locator", locatorEdit.text.toString())
                dismiss()
            }
            locatorNegBtn.setOnClickListener { dismiss() }
        }
    }
}
