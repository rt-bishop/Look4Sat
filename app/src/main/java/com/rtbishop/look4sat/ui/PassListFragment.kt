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

package com.rtbishop.look4sat.ui

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.github.amsacode.predict4java.TLE
import com.rtbishop.look4sat.MainViewModel
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentPassListBinding
import com.rtbishop.look4sat.repo.SatEntry
import com.rtbishop.look4sat.repo.SatPass
import com.rtbishop.look4sat.ui.adapters.SatPassAdapter
import java.util.*
import java.util.concurrent.TimeUnit

class PassListFragment : Fragment(R.layout.fragment_pass_list) {

    private lateinit var viewModel: MainViewModel
    private lateinit var satPassAdapter: SatPassAdapter
    private lateinit var btnPassPrefs: ImageButton
    private lateinit var aosTimer: CountDownTimer
    private lateinit var aosTimerText: TextView
    private lateinit var mainActivity: MainActivity
    private lateinit var satPassList: MutableList<SatPass>
    private var isTimerSet: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragmentBinding = FragmentPassListBinding.bind(view)
        mainActivity = activity as MainActivity
        viewModel = ViewModelProvider(mainActivity).get(MainViewModel::class.java)
        satPassAdapter = SatPassAdapter(viewModel)
        satPassList = mutableListOf()
        aosTimerText = mainActivity.findViewById(R.id.toolbar_timer)
        btnPassPrefs = mainActivity.findViewById(R.id.toolbar_filter)
        setupComponents(fragmentBinding)
        setupObservers(fragmentBinding)
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
        binding.refLayoutPassList.setOnRefreshListener { viewModel.getPasses() }
        btnPassPrefs.setOnClickListener {
            showSatPassPrefsDialog(viewModel.getHoursAhead(), viewModel.getMinElevation())
        }
        binding.fabSatSelect.setOnClickListener {
            showSelectSatDialog(viewModel.tleMainList, viewModel.tleSelection)
        }
    }

    private fun setupObservers(binding: FragmentPassListBinding) {
        viewModel.getSatPassList().observe(viewLifecycleOwner, Observer {
            satPassList = it
            satPassAdapter.setList(satPassList)
            if (satPassList.isNotEmpty()) {
                binding.layoutInfo.visibility = View.INVISIBLE
                binding.recPassList.visibility = View.VISIBLE
            } else {
                binding.layoutInfo.visibility = View.VISIBLE
                binding.recPassList.visibility = View.INVISIBLE
            }
            setTimer()
        })
        viewModel.getRefreshing().observe(viewLifecycleOwner, Observer {
            binding.refLayoutPassList.isRefreshing = it
        })
    }

    private fun showSatPassPrefsDialog(hoursAhead: Int, minEl: Double) {
        val satPassPrefView = View.inflate(mainActivity, R.layout.dialog_pass_pref, null)
        val etHoursAhead = satPassPrefView.findViewById<EditText>(R.id.pref_et_hoursAhead)
        val etMinEl = satPassPrefView.findViewById<EditText>(R.id.pref_et_minEl)
        etHoursAhead.setText(hoursAhead.toString())
        etMinEl.setText(minEl.toString())

        val builder = AlertDialog.Builder(mainActivity)
        builder.setTitle(getString(R.string.dialog_filter_passes))
            .setPositiveButton(getString(R.string.btn_ok)) { _, _ ->
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
                            viewModel.getPasses()
                        }
                    }
                } else Toast.makeText(
                    mainActivity,
                    getString(R.string.err_enter_value),
                    Toast.LENGTH_SHORT
                ).show()

            }
            .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .setView(satPassPrefView)
            .create()
            .show()
    }

    private fun showSelectSatDialog(tleMainList: List<TLE>, selection: MutableList<Int>) {
        if (tleMainList.isEmpty()) {
            Toast.makeText(mainActivity, getString(R.string.err_update_tle), Toast.LENGTH_SHORT)
                .show()
        } else {
            val entriesList = mutableListOf<SatEntry>().apply {
                tleMainList.withIndex().forEach {
                    this.add(SatEntry(it.index, it.value.name))
                }
            }

            val listener = object : SatEntryDialog.EntriesSubmitListener {
                override fun onEntriesSubmit(list: MutableList<Int>) {
                    viewModel.updateAndSaveSelectionList(list)
                    viewModel.getPasses()
                }
            }

            val dialogFragment = SatEntryDialog().apply {
                setEntriesList(entriesList)
                setSelectionList(selection)
                setEntriesListener(listener)
            }
            dialogFragment.show(mainActivity.supportFragmentManager, "SatEntryDialog")
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
                viewModel.getPasses()
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
