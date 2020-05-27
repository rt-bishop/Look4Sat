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

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.Look4SatApp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.dagger.ViewModelFactory
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.SatEntry
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.FragmentPassListBinding
import com.rtbishop.look4sat.ui.MainActivity
import com.rtbishop.look4sat.ui.SharedViewModel
import com.rtbishop.look4sat.ui.adapters.SatPassAdapter
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PassListFragment : Fragment(R.layout.fragment_pass_list) {

    @Inject
    lateinit var modelFactory: ViewModelFactory
    private lateinit var viewModel: SharedViewModel
    private lateinit var satPassAdapter: SatPassAdapter
    private lateinit var btnPassPrefs: ImageButton
    private lateinit var aosTimer: CountDownTimer
    private lateinit var aosTimerText: TextView
    private lateinit var mainActivity: MainActivity
    private var isTimerSet: Boolean = false
    private var satPassList: MutableList<SatPass> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPassListBinding.bind(view)
        mainActivity = activity as MainActivity
        (mainActivity.application as Look4SatApp).appComponent.inject(this)
        viewModel = ViewModelProvider(mainActivity, modelFactory).get(SharedViewModel::class.java)
        satPassAdapter = SatPassAdapter(satPassList)
        aosTimerText = mainActivity.findViewById(R.id.toolbar_timer)
        btnPassPrefs = mainActivity.findViewById(R.id.toolbar_filter)

        setupObservers(binding)
        setupComponents(binding)

        if (viewModel.isFirstLaunch) {
            viewModel.calculatePasses()
            viewModel.isFirstLaunch = false
        }
    }

    private fun setupObservers(binding: FragmentPassListBinding) {
        viewModel.getPassList().observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Result.Success -> {
                    satPassList = result.data
                    satPassAdapter.setList(satPassList)
                    if (satPassList.isNotEmpty()) {
                        binding.layoutInfo.visibility = View.INVISIBLE
                        binding.recPassList.visibility = View.VISIBLE
                    } else {
                        binding.layoutInfo.visibility = View.VISIBLE
                        binding.recPassList.visibility = View.INVISIBLE
                    }
                    setTimer()
                    binding.refLayoutPassList.isRefreshing = false
                }
                is Result.InProgress -> {
                    binding.refLayoutPassList.isRefreshing = true
                }
            }
        })
    }

    private fun setupComponents(binding: FragmentPassListBinding) {
        binding.recPassList.apply {
            layoutManager = LinearLayoutManager(mainActivity)
            adapter = satPassAdapter
            isVerticalScrollBarEnabled = false
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }
        satPassAdapter.setList(satPassList)
        setTimer()

        binding.refLayoutPassList.setProgressBackgroundColorSchemeResource(R.color.themeAccent)
        binding.refLayoutPassList.setColorSchemeResources(R.color.backgroundDark)
        binding.refLayoutPassList.setOnRefreshListener { viewModel.calculatePasses() }
        btnPassPrefs.setOnClickListener {
            showSatPassPrefsDialog(viewModel.getHoursAhead(), viewModel.getMinElevation())
        }
        binding.fabSatSelect.setOnClickListener {
            lifecycleScope.launch {
                val list = viewModel.getAllEntries() as MutableList
                if (list.isEmpty()) {
                    Toast.makeText(requireContext(), "Please, update TLE", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    showSelectSatDialog(list, binding)
                }
            }
        }
    }

    private fun showSatPassPrefsDialog(hoursAhead: Int, minEl: Double) {
        val satPassPrefView = View.inflate(mainActivity, R.layout.dialog_pass_pref, null)
        val etHoursAhead = satPassPrefView.findViewById<EditText>(R.id.pref_et_hoursAhead)
        val etMinEl = satPassPrefView.findViewById<EditText>(R.id.pref_et_minEl)
        etHoursAhead.setText(hoursAhead.toString())
        etMinEl.setText(minEl.toString())

        AlertDialog.Builder(mainActivity).apply {
            setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                val hoursStr = etHoursAhead.text.toString()
                val elevationStr = etMinEl.text.toString()
                if (hoursStr.isNotEmpty() && elevationStr.isNotEmpty()) {
                    val hours = hoursStr.toInt()
                    val elevation = elevationStr.toDouble()
                    when {
                        hours < 1 || hours > 168 -> {
                            Toast.makeText(
                                mainActivity,
                                getString(R.string.pref_hours_ahead_input_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        elevation < 0 || elevation > 90 -> {
                            Toast.makeText(
                                mainActivity,
                                getString(R.string.pref_min_el_input_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            viewModel.setPassPrefs(hours, elevation)
                            viewModel.calculatePasses()
                        }
                    }
                } else Toast.makeText(
                    mainActivity,
                    getString(R.string.err_enter_value),
                    Toast.LENGTH_SHORT
                ).show()

            }
            setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            setView(satPassPrefView)
            create()
            show()
        }
    }

    private fun showSelectSatDialog(
        tleMainList: MutableList<SatEntry>,
        binding: FragmentPassListBinding
    ) {
        val listener = object : SatEntryDialogFragment.EntriesSubmitListener {
            override fun onEntriesSubmit(catNumList: MutableList<Int>) {
                binding.refLayoutPassList.isRefreshing = true
                viewModel.updateEntriesSelection(catNumList)
            }
        }

        SatEntryDialogFragment(tleMainList).apply {
            setEntriesListener(listener)
            show(mainActivity.supportFragmentManager, "SatEntryDialogFragment")
        }
    }

    private fun setTimer() {
        if (satPassList.isNotEmpty()) {
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
        val nextPass = satPassList.first { it.pass.startTime.after(timeNow) }
        val millisBeforeStart = nextPass.pass.startTime.time.minus(timeNow.time)
        aosTimer = object : CountDownTimer(millisBeforeStart, 1000) {
            override fun onFinish() {
                setTimer()
            }

            override fun onTick(millisUntilFinished: Long) {
                aosTimerText.text = String.format(
                    mainActivity.getString(R.string.pat_timer),
                    TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 60,
                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                )
                satPassAdapter.updateRecycler()
            }
        }
    }

    private fun setTimerForLast(timeNow: Date) {
        val lastPass = satPassList.last()
        val millisBeforeEnd = lastPass.pass.endTime.time.minus(timeNow.time)
        aosTimer = object : CountDownTimer(millisBeforeEnd, 1000) {
            override fun onFinish() {
                viewModel.calculatePasses()
            }

            override fun onTick(millisUntilFinished: Long) {
                aosTimerText.text = String.format(
                    mainActivity.getString(R.string.pat_timer),
                    TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 60,
                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                )
                satPassAdapter.updateRecycler()
            }
        }
    }

    private fun resetTimer(resetToNull: Boolean = false) {
        if (isTimerSet) {
            aosTimer.cancel()
            isTimerSet = false
        }
        if (resetToNull) aosTimerText.text =
            String.format(getString(R.string.pat_timer), 0, 0, 0)
    }
}
