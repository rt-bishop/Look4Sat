package com.rtbishop.lookingsat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
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

class SkyFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var recViewCurrent: RecyclerView
    private lateinit var recViewFuture: RecyclerView
    private lateinit var recAdapterCurrent: RecyclerView.Adapter<*>
    private lateinit var recAdapterFuture: RecyclerView.Adapter<*>
    private lateinit var btnPassPrefs: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var fab: FloatingActionButton

    private var satPassList: List<SatPass> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sky, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        btnPassPrefs = (activity as MainActivity).findViewById(R.id.toolbar_btn_refresh)
        progressBar = view.findViewById(R.id.sky_progressbar)
        recViewFuture = view.findViewById(R.id.sky_recycler_future)
        fab = view.findViewById(R.id.sky_fab)

        recViewFuture.layoutManager = LinearLayoutManager(activity)

        fab.setOnClickListener { showSelectSatDialog() }
    }

    private fun showSelectSatDialog() {
        val tleSelectedMap = viewModel.tleSelectedMap
        val tleMainListSize = viewModel.tleMainList.size

        val tleNameArray = arrayOfNulls<String>(tleMainListSize)
        val tleCheckedArray = BooleanArray(tleMainListSize)

        if (tleSelectedMap.isEmpty()) {
            for ((position, tle) in viewModel.tleMainList.withIndex()) {
                tleNameArray[position] = tle.name
                tleCheckedArray[position] = false
            }
        } else {
            for ((position, tle) in viewModel.tleMainList.withIndex()) {
                tleNameArray[position] = tle.name
                tleCheckedArray[position] = viewModel.tleSelectedMap.getOrDefault(tle, false)
            }
        }

        val selectionMap = mutableMapOf<TLE, Boolean>()
        val builder = AlertDialog.Builder(activity as MainActivity)
        builder.setTitle("Select satellites to track")
            .setMultiChoiceItems(tleNameArray, tleCheckedArray) { _, which, isChecked ->
                selectionMap[viewModel.tleMainList[which]] = isChecked
            }
            .setPositiveButton("Ok") { _, _ ->
                for ((tle, value) in selectionMap) {
                    tleSelectedMap[tle] = value
                    calculatePasses()
                }
            }
            .setNeutralButton("Clear All") { _, _ ->
                tleSelectedMap.clear()
                calculatePasses()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }

    private fun calculatePasses() {
        lifecycleScope.launch(Dispatchers.Main) {
            recViewFuture.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
            progressBar.isIndeterminate = true
            satPassList = viewModel.getPassesForSelectedSatellites()
            recAdapterFuture = SatPassAdapter(satPassList)
            recViewFuture.adapter = recAdapterFuture
            progressBar.isIndeterminate = false
            progressBar.visibility = View.INVISIBLE
            recViewFuture.visibility = View.VISIBLE
        }
    }
}