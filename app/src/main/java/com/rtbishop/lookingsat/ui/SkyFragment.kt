package com.rtbishop.lookingsat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.lookingsat.R
import com.rtbishop.lookingsat.vm.MainViewModel

class SkyFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var recView: RecyclerView
    private lateinit var recAdapter: RecyclerView.Adapter<*>
    private lateinit var recLayoutManager: RecyclerView.LayoutManager
    private lateinit var btnRefresh: ImageButton

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

        btnRefresh.setOnClickListener {
            val satPassList = viewModel.updateRecycler()

            recLayoutManager = LinearLayoutManager(activity)
            recAdapter = SatPassAdapter(satPassList)
            recView = view.findViewById(R.id.recycler_sky)

            recView.setHasFixedSize(true)
            recView.layoutManager = recLayoutManager
            recView.adapter = recAdapter
        }
    }
}