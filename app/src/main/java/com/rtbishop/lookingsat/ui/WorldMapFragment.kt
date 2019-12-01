package com.rtbishop.lookingsat.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.amsacode.predict4java.PassPredictor
import com.github.amsacode.predict4java.TLE
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rtbishop.lookingsat.MainViewModel
import com.rtbishop.lookingsat.R
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WorldMapFragment : Fragment() {

    private val delay = 3000L
    private val service = Executors.newSingleThreadScheduledExecutor()

    private lateinit var viewModel: MainViewModel
    private lateinit var trackView: TrackView
    private lateinit var mapFrame: FrameLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var selectedSat: TLE
    private lateinit var predictor: PassPredictor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_worldmap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFrame = view.findViewById(R.id.worldmap_frame)
        fab = view.findViewById(R.id.worldmap_fab)
        fab.setOnClickListener { showSelectSatDialog() }

        selectedSat = viewModel.selectedSingleSat
        lifecycleScope.launch {
            predictor = PassPredictor(selectedSat, viewModel.gsp.value)
        }

        trackView = TrackView(activity as MainActivity)
        mapFrame.addView(trackView)

        service.scheduleAtFixedRate({ trackView.invalidate() }, delay, delay, TimeUnit.MILLISECONDS)
    }

    private fun showSelectSatDialog() {
        val tleMainList = viewModel.tleMainList
        val tleNameArray = arrayOfNulls<String>(tleMainList.size)
        var selection = viewModel.selectedSingleSat

        tleMainList.withIndex().forEach { (position, tle) ->
            tleNameArray[position] = tle.name
        }

        val builder = AlertDialog.Builder(activity as MainActivity)
        builder.setTitle("Select Sat to track")
            .setSingleChoiceItems(tleNameArray, -1) { _, which ->
                selection = tleMainList[which]
            }
            .setPositiveButton("Ok") { _, _ ->
                viewModel.updateSelectedSingleSat(selection)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }

    inner class TrackView(context: Context) : View(context)
}