package com.rtbishop.look4sat.presentation.settingsScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.ISettingsHandler
import com.rtbishop.look4sat.databinding.DialogLocationBinding
import com.rtbishop.look4sat.presentation.setNavResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationDialog : AppCompatDialogFragment() {

    @Inject
    lateinit var preferences: ISettingsHandler

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_location, group, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        DialogLocationBinding.bind(view).run {
            dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
            dialog?.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.94).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            val location = preferences.loadStationPosition()
            locationLatEdit.setText(location.latitude.toString())
            locationLonEdit.setText(location.longitude.toString())
            locationPosBtn.setOnClickListener {
                val latitude = try {
                    locationLatEdit.text.toString().toDouble()
                } catch (exception: Exception) {
                    180.0
                }
                val longitude = try {
                    locationLonEdit.text.toString().toDouble()
                } catch (exception: Exception) {
                    400.0
                }
                setNavResult("location", Pair(latitude, longitude))
                dismiss()
            }
            locationNegBtn.setOnClickListener { dismiss() }
        }
    }
}
