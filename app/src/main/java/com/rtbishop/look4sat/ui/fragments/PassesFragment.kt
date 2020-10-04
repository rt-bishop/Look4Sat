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

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.Look4SatApp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.dagger.ViewModelFactory
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.FragmentPassesBinding
import com.rtbishop.look4sat.ui.SharedViewModel
import com.rtbishop.look4sat.ui.adapters.PassesAdapter
import com.rtbishop.look4sat.utility.PrefsManager
import com.rtbishop.look4sat.utility.Utilities
import com.rtbishop.look4sat.utility.Utilities.getRotationAnimator
import com.rtbishop.look4sat.utility.Utilities.snack
import java.util.*
import javax.inject.Inject

class PassesFragment : Fragment(R.layout.fragment_passes) {

    @Inject
    lateinit var factory: ViewModelFactory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var aosTimer: CountDownTimer
    private lateinit var binding: FragmentPassesBinding
    private lateinit var animator: ObjectAnimator
    private val viewModel: SharedViewModel by activityViewModels { factory }
    private val passesAdapter = PassesAdapter()
    private var isTimerSet: Boolean = false
    private var passes = mutableListOf<SatPass>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPassesBinding.bind(view)
        (requireActivity().application as Look4SatApp).appComponent.inject(this)
        setupComponents()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.getPasses().observe(viewLifecycleOwner, { result ->
            when (result) {
                is Result.Success -> {
                    animator.cancel()
                    passes = result.data
                    passesAdapter.setList(passes)
                    setTimerForPasses(passes)
                }
                is Result.InProgress -> animator.start()
            }
        })
    }

    private fun setupComponents() {
        binding.apply {
            passesRecycler.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = passesAdapter
                isVerticalScrollBarEnabled = false
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                setHasFixedSize(true)
            }
            passesFilter.setOnClickListener {
                val passPrefs = prefsManager.getPassPrefs()
                showSatPassPrefsDialog(passPrefs.hoursAhead, passPrefs.minEl)
            }
            animator = passesFab.getRotationAnimator()
            passesFab.setOnClickListener {
                if (animator.isRunning) {
                    animator.cancel()
                } else {
                    viewModel.calculatePasses()
                }
            }
        }
        passesAdapter.setShouldUseUTC(prefsManager.shouldUseUTC())
        setTimerForPasses(passes)
        viewModel.triggerCalculation()
    }

    private fun showSatPassPrefsDialog(hoursAhead: Int, minEl: Double) {
        val satPassPrefView = View.inflate(requireContext(), R.layout.dialog_passes, null)
        val etHoursAhead = satPassPrefView.findViewById<EditText>(R.id.pref_et_hoursAhead)
        val etMinEl = satPassPrefView.findViewById<EditText>(R.id.pref_et_minEl)
        etHoursAhead.setText(hoursAhead.toString())
        etMinEl.setText(minEl.toString())

        AlertDialog.Builder(requireContext()).apply {
            setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                val hoursString = etHoursAhead.text.toString()
                val minElString = etMinEl.text.toString()
                if (hoursString.isNotEmpty() && minElString.isNotEmpty()) {
                    val hours = hoursString.toInt()
                    val elevation = minElString.toDouble()
                    when {
                        hours < 1 || hours > 168 -> {
                            getString(R.string.pref_hours_ahead_input_error).snack(requireView())
                        }
                        elevation < 0 || elevation > 90 -> {
                            getString(R.string.pref_min_el_input_error).snack(requireView())
                        }
                        else -> {
                            prefsManager.setPassPrefs(hours, elevation)
                            viewModel.calculatePasses()
                        }
                    }
                } else getString(R.string.err_enter_value).snack(requireView())

            }
            setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            setView(satPassPrefView)
            create()
            show()
        }
    }

    private fun setTimerForPasses(passes: List<SatPass>) {
        if (passes.isNotEmpty()) {
            resetTimer()
            val timeNow = Date()
            try {
                setTimerForNext(timeNow)
            } catch (e: NoSuchElementException) {
                setTimerForLast(timeNow)
            } finally {
                aosTimer.start()
                isTimerSet = true
            }
        } else {
            resetTimer(true)
        }
    }

    private fun setTimerForNext(timeNow: Date) {
        val nextPass = passes.first { it.pass.startTime.after(timeNow) }
        val millisBeforeStart = nextPass.pass.startTime.time.minus(timeNow.time)
        aosTimer = object : CountDownTimer(millisBeforeStart, 1000) {
            override fun onFinish() {
                setTimerForPasses(passes)
            }

            override fun onTick(millisUntilFinished: Long) {
                binding.passesTimer.text = Utilities.formatForTimer(millisUntilFinished)
                passesAdapter.updateRecycler()
            }
        }
    }

    private fun setTimerForLast(timeNow: Date) {
        val lastPass = passes.last()
        val millisBeforeEnd = lastPass.pass.endTime.time.minus(timeNow.time)
        aosTimer = object : CountDownTimer(millisBeforeEnd, 1000) {
            override fun onFinish() {
                viewModel.calculatePasses()
            }

            override fun onTick(millisUntilFinished: Long) {
                binding.passesTimer.text = Utilities.formatForTimer(millisUntilFinished)
                passesAdapter.updateRecycler()
            }
        }
    }

    private fun resetTimer(resetToNull: Boolean = false) {
        if (isTimerSet) {
            aosTimer.cancel()
            isTimerSet = false
        }
        if (resetToNull) binding.passesTimer.text = String.format("%02d:%02d:%02d", 0, 0, 0)
    }
}
