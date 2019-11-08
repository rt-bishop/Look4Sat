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
import com.rtbishop.lookingsat.R
import com.rtbishop.lookingsat.repo.SatPass
import com.rtbishop.lookingsat.vm.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SkyFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var recView: RecyclerView
    private lateinit var recAdapter: RecyclerView.Adapter<*>
    private lateinit var btnRefresh: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var fab: FloatingActionButton

    private var satPassList: List<SatPass> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sky, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        btnRefresh = (activity as MainActivity).findViewById(R.id.toolbar_btn_refresh)
        progressBar = view.findViewById(R.id.sky_progressbar)
        recView = view.findViewById(R.id.sky_recycler)
        fab = view.findViewById(R.id.sky_fab)

        recView.layoutManager = LinearLayoutManager(activity)
        recView.setHasFixedSize(true)

        btnRefresh.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                recView.visibility = View.INVISIBLE
                progressBar.visibility = View.VISIBLE
                progressBar.isIndeterminate = true
                satPassList = viewModel.updateRecycler()
                recAdapter = SatPassAdapter(satPassList)
                recView.adapter = recAdapter
                progressBar.isIndeterminate = false
                progressBar.visibility = View.INVISIBLE
                recView.visibility = View.VISIBLE
            }
        }

        fab.setOnClickListener { showSelectSatDialog() }
    }

    private var selectedSatList: List<TLE> = emptyList()

    private fun showSelectSatDialog() {
        val tleList = mutableListOf<TLE>()
        val listSize = viewModel.tleList.size
        val satList = arrayOfNulls<String>(listSize)
        val checkedSatList = BooleanArray(listSize)
        if (selectedSatList.isEmpty()) {
            for ((position, tle) in viewModel.tleList.withIndex()) {
                satList[position] = tle.name
                checkedSatList[position] = false
            }
        }

        val builder = AlertDialog.Builder(activity as MainActivity)
        builder.setTitle("Select satellites to track")

        builder.setMultiChoiceItems(satList, checkedSatList) { _, which, isChecked ->
            checkedSatList[which] = isChecked
        }

        builder.setPositiveButton("Select") { _, _ ->
            for (index in checkedSatList.indices) {
                val checked = checkedSatList[index]
                if (checked) {
                    tleList.add(viewModel.tleList[index])
                }
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        val dialog = builder.create()
        dialog.show()
    }
}