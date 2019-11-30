package com.rtbishop.lookingsat.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.lookingsat.R
import com.rtbishop.lookingsat.repo.SatPass
import java.text.SimpleDateFormat
import java.util.*

class SatPassAdapter : RecyclerView.Adapter<SatPassAdapter.ViewHolder>() {

    private var satPassList = emptyList<SatPass>()

    fun setList(list: List<SatPass>) {
        satPassList = list
    }

    override fun getItemCount(): Int {
        return satPassList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.sat_pass_card, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val satPass = satPassList[position]

        val satName = holder.itemView.findViewById<TextView>(R.id.card_sat_name)
        val maxEl = holder.itemView.findViewById<TextView>(R.id.card_max_el)
        val passVector = holder.itemView.findViewById<TextView>(R.id.card_pass_vector)
        val passStart = holder.itemView.findViewById<TextView>(R.id.card_pass_start)
        val passEnd = holder.itemView.findViewById<TextView>(R.id.card_pass_end)

        val aosTime =
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(satPass.pass.startTime)
        val losTime =
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(satPass.pass.endTime)
        satName.text = satPass.tle.name
        maxEl.text = String.format("%.1f°", satPass.pass.maxEl)
        passVector.text =
            String.format("Az: %2d° -> %2d°", satPass.pass.aosAzimuth, satPass.pass.losAzimuth)
        passStart.text = String.format("AOS - %s", aosTime)
        passEnd.text = String.format("LOS - %s", losTime)

        holder.itemView.setOnClickListener {
            val bundle = bundleOf("satPass" to satPass)
            holder.itemView.findNavController().navigate(R.id.action_nav_sky_to_nav_radar, bundle)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}