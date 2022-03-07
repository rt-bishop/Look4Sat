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
package com.rtbishop.look4sat.presentation.passesScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.DialogFilterBinding
import com.rtbishop.look4sat.domain.ISettingsManager
import com.rtbishop.look4sat.presentation.setNavResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FilterDialog : AppCompatDialogFragment() {

    @Inject
    lateinit var preferences: ISettingsManager

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
                    24
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
