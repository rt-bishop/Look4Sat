package com.rtbishop.lookingsat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.lookingsat.R
import com.rtbishop.lookingsat.repo.SatPass

class RadarFragment : Fragment() {

    private lateinit var satPass: SatPass
    private lateinit var radarSkyFrame: FrameLayout
    private lateinit var radarRecycler: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_radar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        satPass = arguments?.get("satPass") as SatPass
        radarSkyFrame = view.findViewById(R.id.radar_sky_frame)
        radarRecycler = view.findViewById(R.id.radar_recycler)
    }
}