package com.rtbishop.lookingsat.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.amsacode.predict4java.TLE
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rtbishop.lookingsat.MainViewModel
import com.rtbishop.lookingsat.R
import com.rtbishop.lookingsat.repo.SatPass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SkyFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var recViewCurrent: RecyclerView
    private lateinit var recViewFuture: RecyclerView
    private lateinit var recAdapterCurrent: RecyclerView.Adapter<*>
    private lateinit var recAdapterFuture: SatPassAdapter
    private lateinit var timeToAos: TextView
    private lateinit var btnPassPrefs: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var fab: FloatingActionButton
    private lateinit var aosTimer: CountDownTimer
    private lateinit var satPassList: List<SatPass>
    private var isTimerSet: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        recAdapterFuture = SatPassAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sky, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
    }

    private fun setupViews(view: View) {
        timeToAos = (activity as MainActivity).findViewById(R.id.toolbar_time_to_aos)
        btnPassPrefs = (activity as MainActivity).findViewById(R.id.toolbar_btn_refresh)
        progressBar = view.findViewById(R.id.sky_progressbar)
        recViewFuture = view.findViewById(R.id.sky_recycler_future)
        fab = view.findViewById(R.id.sky_fab)

        satPassList = viewModel.satPassList
        recViewFuture.apply {
            layoutManager = LinearLayoutManager(activity as MainActivity)
            adapter = recAdapterFuture
        }
        recAdapterFuture.setList(satPassList)
        recAdapterFuture.notifyDataSetChanged()
        if (satPassList.isEmpty()) {
            resetTimer()
        }

        btnPassPrefs.setOnClickListener { showSatPassPrefsDialog() }
        fab.setOnClickListener { showSelectSatDialog() }
    }

    private fun showSatPassPrefsDialog() {
        val context = activity as MainActivity
        val satPassPrefView = View.inflate(context, R.layout.sat_pass_pref, null)
        val etHours = satPassPrefView.findViewById<EditText>(R.id.pass_pref_et_hours)
        val etMaxEl = satPassPrefView.findViewById<EditText>(R.id.pass_pref_et_maxEl)
        etHours.setText(viewModel.passPrefs.hoursAhead.toString())
        etMaxEl.setText(viewModel.passPrefs.maxEl.toString())

        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.title_pass_pref))
            .setPositiveButton(getString(R.string.btn_ok)) { _, _ ->
                val hoursAhead = etHours.text.toString().toInt()
                val maxEl = etMaxEl.text.toString().toDouble()
                viewModel.updatePassPrefs(hoursAhead, maxEl)
                calculatePasses()
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .setView(satPassPrefView)
            .create()
            .show()
    }

    private fun showSelectSatDialog() {
        val tleMainList: List<TLE> = viewModel.tleMainList
        val tleNameArray = arrayOfNulls<String>(tleMainList.size)
        val tleCheckedArray = BooleanArray(tleMainList.size)
        val selectedSatMap = viewModel.tleSelectedMap
        val currentSelectionMap = mutableMapOf<TLE, Boolean>()

        if (selectedSatMap.isEmpty()) {
            tleMainList.withIndex().forEach { (position, tle) ->
                tleNameArray[position] = tle.name
                tleCheckedArray[position] = false
            }
        } else {
            tleMainList.withIndex().forEach { (position, tle) ->
                tleNameArray[position] = tle.name
                tleCheckedArray[position] = selectedSatMap.getOrDefault(tle, false)
            }
        }

        val builder = AlertDialog.Builder(activity as MainActivity)
        builder.setTitle(getString(R.string.title_select_sat))
            .setMultiChoiceItems(tleNameArray, tleCheckedArray) { _, which, isChecked ->
                currentSelectionMap[tleMainList[which]] = isChecked
            }
            .setPositiveButton(getString(R.string.btn_ok)) { _, _ ->
                for ((tle, value) in currentSelectionMap) {
                    selectedSatMap[tle] = value
                    viewModel.updateSelectedSatMap(selectedSatMap)
                    calculatePasses()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .setNeutralButton(getString(R.string.btn_clear)) { _, _ ->
                selectedSatMap.clear()
                viewModel.updateSelectedSatMap(selectedSatMap)
                calculatePasses()
            }
            .create()
            .show()
    }

    private fun calculatePasses() {
        lifecycleScope.launch(Dispatchers.Main) {
            recViewFuture.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
            progressBar.isIndeterminate = true
            viewModel.getPasses()
            satPassList = viewModel.satPassList
            recAdapterFuture.setList(satPassList)
            recAdapterFuture.notifyDataSetChanged()
            progressBar.isIndeterminate = false
            progressBar.visibility = View.INVISIBLE
            recViewFuture.visibility = View.VISIBLE
            if (satPassList.isNotEmpty()) setTimer(satPassList.first().pass.startTime.time)
            else resetTimer()
        }
    }

    private fun setTimer(passTime: Long) {
        if (isTimerSet) resetTimer()
        val totalMillis = passTime.minus(System.currentTimeMillis())
        aosTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onFinish() {
                Toast.makeText(activity, "Time is up!", Toast.LENGTH_SHORT).show()
                this.cancel()
            }

            override fun onTick(millisUntilFinished: Long) {
                timeToAos.text = String.format(
                    getString(R.string.pattern_aos),
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
        }
        timeToAos.text = String.format(getString(R.string.pattern_aos), 0, 0, 0)
    }
}