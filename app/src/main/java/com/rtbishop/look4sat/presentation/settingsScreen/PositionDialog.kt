package com.rtbishop.look4sat.presentation.settingsScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.DialogPositionBinding
import com.rtbishop.look4sat.domain.ISettings
import com.rtbishop.look4sat.presentation.setNavResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PositionDialog : AppCompatDialogFragment() {

    @Inject
    lateinit var preferences: ISettings

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_position, group, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        DialogPositionBinding.bind(view).run {
            dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
            dialog?.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.94).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            val location = preferences.loadStationPosition()
            positionLatEdit.setText(location.latitude.toString())
            positionLonEdit.setText(location.longitude.toString())
            positionBtnPos.setOnClickListener {
                val latitude = try {
                    positionLatEdit.text.toString().toDouble()
                } catch (exception: Exception) {
                    180.0
                }
                val longitude = try {
                    positionLonEdit.text.toString().toDouble()
                } catch (exception: Exception) {
                    400.0
                }
                setNavResult("position", Pair(latitude, longitude))
                dismiss()
            }
            positionBtnNeg.setOnClickListener { dismiss() }
        }
    }
}
