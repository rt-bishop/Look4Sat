/*
 * LookingSat. Amateur radio & weather satellite tracker and passes calculator.
 * Copyright (C) 2019 Arty Bishop (bishop.arty@gmail.com)
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.amsacode.predict4java.TLE
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rtbishop.look4sat.MainViewModel
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.repo.SatPass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SkyFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var recyclerPasses: RecyclerView
    private lateinit var recyclerAdapter: SatPassAdapter
    private lateinit var timeToAos: TextView
    private lateinit var btnPassPrefs: ImageButton
    private lateinit var fab: FloatingActionButton
    private lateinit var aosTimer: CountDownTimer
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var mainActivity: MainActivity
    private lateinit var satPassList: List<SatPass>
    private var isTimerSet: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = activity as MainActivity
        viewModel = ViewModelProvider(mainActivity).get(MainViewModel::class.java)
        recyclerAdapter = SatPassAdapter()
        satPassList = emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sky, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViews(view)
        setupComponents()
        setupObservers()
    }

    private fun findViews(view: View) {
        timeToAos = (mainActivity).findViewById(R.id.toolbar_time_to_aos)
        btnPassPrefs = (mainActivity).findViewById(R.id.toolbar_btn_refresh)
        swipeLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerPasses = view.findViewById(R.id.sky_recycler_future)
        fab = view.findViewById(R.id.sky_fab)
    }

    private fun setupComponents() {
        recyclerPasses.apply {
            layoutManager = LinearLayoutManager(mainActivity)
            adapter = recyclerAdapter
        }
        recyclerAdapter.setList(satPassList)
        recyclerAdapter.notifyDataSetChanged()
        if (satPassList.isEmpty()) resetTimer()

        swipeLayout.setProgressBackgroundColorSchemeResource(R.color.themeAccent)
        swipeLayout.setColorSchemeResources(R.color.darkOnLight)
        swipeLayout.setOnRefreshListener { calculatePasses() }
        btnPassPrefs.setOnClickListener { showSatPassPrefsDialog() }
        fab.setOnClickListener {
            showSelectSatDialog(viewModel.tleMainList, viewModel.tleSelection)
        }
    }

    private fun setupObservers() {
        viewModel.passSatList.observe(viewLifecycleOwner, Observer {
            satPassList = it
            recyclerAdapter.setList(satPassList)
            recyclerAdapter.notifyDataSetChanged()
            if (satPassList.isNotEmpty()) setTimer(satPassList.first().pass.startTime.time)
            else {
                resetTimer()
                timeToAos.text = String.format(getString(R.string.pattern_aos), 0, 0, 0)
            }
            swipeLayout.isRefreshing = false
        })
    }

    private fun showSatPassPrefsDialog() {
        val satPassPrefView = View.inflate(mainActivity, R.layout.sat_pass_pref, null)
        val etHoursAhead = satPassPrefView.findViewById<EditText>(R.id.pref_hours_ahead_et)
        val etMinEl = satPassPrefView.findViewById<EditText>(R.id.pref_min_el_et)
        etHoursAhead.setText(viewModel.hoursAhead.toString())
        etMinEl.setText(viewModel.minEl.toString())

        val builder = AlertDialog.Builder(mainActivity)
        builder.setTitle(getString(R.string.dialog_pass_prefs))
            .setPositiveButton(getString(R.string.btn_ok)) { _, _ ->
                val hoursAhead = etHoursAhead.text.toString().toInt()
                val minEl = etMinEl.text.toString().toDouble()
                viewModel.setPassPrefs(hoursAhead, minEl)
                calculatePasses()
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
            Toast.makeText(mainActivity, "Please, update your TLE", Toast.LENGTH_SHORT).show()
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

    private fun calculatePasses() {
        swipeLayout.isRefreshing = true
        lifecycleScope.launch(Dispatchers.Main) { viewModel.getPasses() }
    }

    private fun setTimer(passTime: Long) {
        resetTimer()
        val totalMillis = passTime.minus(System.currentTimeMillis())
        aosTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onFinish() {
                Toast.makeText(activity, "Time is up!", Toast.LENGTH_SHORT).show()
                this.cancel()
            }

            override fun onTick(millisUntilFinished: Long) {
                timeToAos.text = String.format(
                    mainActivity.getString(R.string.pattern_aos),
                    TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 60,
                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                )
            }
        }
        aosTimer.start()
        isTimerSet = true
    }

    private fun resetTimer() {
        if (isTimerSet) {
            aosTimer.cancel()
            isTimerSet = false
        } else timeToAos.text = String.format(getString(R.string.pattern_aos), 0, 0, 0)
    }
}