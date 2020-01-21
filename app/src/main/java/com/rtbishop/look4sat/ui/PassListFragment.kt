/*
 * Look4Sat. Amateur radio & weather satellites passes calculator for Android.
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.amsacode.predict4java.TLE
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rtbishop.look4sat.MainViewModel
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.repo.SatPass
import com.rtbishop.look4sat.ui.adapters.SatPassAdapter
import java.util.*
import java.util.concurrent.TimeUnit

class PassListFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var satPassRecycler: RecyclerView
    private lateinit var satPassAdapter: SatPassAdapter
    private lateinit var btnPassPrefs: ImageButton
    private lateinit var passListFab: FloatingActionButton
    private lateinit var aosTimer: CountDownTimer
    private lateinit var aosTimerText: TextView
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var infoLayout: ConstraintLayout
    private lateinit var mainActivity: MainActivity
    private lateinit var satPassList: MutableList<SatPass>
    private var isTimerSet: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = activity as MainActivity
        viewModel = ViewModelProvider(mainActivity).get(MainViewModel::class.java)
        satPassAdapter = SatPassAdapter()
        satPassList = mutableListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pass_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViews(view)
        setupComponents()
        setupObservers()
    }

    private fun findViews(view: View) {
        aosTimerText = mainActivity.findViewById(R.id.toolbar_timer)
        btnPassPrefs = mainActivity.findViewById(R.id.toolbar_filter)
        swipeLayout = view.findViewById(R.id.pass_list_refresh)
        infoLayout = view.findViewById(R.id.pass_info_layout)
        satPassRecycler = view.findViewById(R.id.pass_list_recycler)
        passListFab = view.findViewById(R.id.pass_list_fab)
    }

    private fun setupComponents() {
        satPassRecycler.apply {
            layoutManager = LinearLayoutManager(mainActivity)
            adapter = satPassAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }
        satPassAdapter.setList(satPassList)
        setTimer()

        swipeLayout.setProgressBackgroundColorSchemeResource(R.color.themeAccent)
        swipeLayout.setColorSchemeResources(R.color.backgroundDark)
        swipeLayout.setOnRefreshListener { calculatePasses() }
        btnPassPrefs.setOnClickListener {
            showSatPassPrefsDialog(viewModel.hoursAhead, viewModel.minEl)
        }
        passListFab.setOnClickListener {
            showSelectSatDialog(viewModel.tleMainList, viewModel.tleSelection)
        }
    }

    private fun setupObservers() {
        viewModel.passSatList.observe(viewLifecycleOwner, Observer {
            satPassList = it
            satPassAdapter.setList(satPassList)
            if (satPassList.isNotEmpty()) {
                infoLayout.visibility = View.INVISIBLE
                satPassRecycler.visibility = View.VISIBLE
            } else {
                infoLayout.visibility = View.VISIBLE
                satPassRecycler.visibility = View.INVISIBLE
            }
            setTimer()
            swipeLayout.isRefreshing = false
        })
    }

    private fun calculatePasses() {
        swipeLayout.isRefreshing = true
        viewModel.getPasses()
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
                            calculatePasses()
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

    private fun showSelectSatDialog(tleMainList: List<TLE>, selectionList: MutableList<Int>) {
        if (tleMainList.isEmpty()) {
            Toast.makeText(mainActivity, getString(R.string.err_update_tle), Toast.LENGTH_SHORT)
                .show()
        } else {
            val tleNameArray = arrayOfNulls<String>(tleMainList.size).apply {
                tleMainList.withIndex().forEach { (position, tle) -> this[position] = tle.name }
            }
            val tleCheckedArray = BooleanArray(tleMainList.size).apply {
                selectionList.forEach { this[it] = true }
            }
            val builder = AlertDialog.Builder(mainActivity)
            builder.setTitle(getString(R.string.dialog_select_sat))
                .setMultiChoiceItems(tleNameArray, tleCheckedArray) { _, which, isChecked ->
                    if (isChecked) selectionList.add(which)
                    else if (selectionList.contains(which)) selectionList.remove(which)
                }
                .setPositiveButton(getString(R.string.btn_ok)) { _, _ ->
                    viewModel.updateAndSaveSelectionList(selectionList)
                    calculatePasses()
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ ->
                    dialog.cancel()
                }
                .setNeutralButton(getString(R.string.btn_clear)) { _, _ ->
                    selectionList.clear()
                    viewModel.updateAndSaveSelectionList(selectionList)
                    calculatePasses()
                }
                .create()
                .show()
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
                    mainActivity.getString(R.string.pat_timer_aos),
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
                calculatePasses()
            }

            override fun onTick(millisUntilFinished: Long) {
                aosTimerText.text = String.format(
                    mainActivity.getString(R.string.pat_timer_los),
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
            String.format(getString(R.string.pat_timer_aos), 0, 0, 0)
    }
}