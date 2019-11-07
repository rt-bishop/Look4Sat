package com.rtbishop.lookingsat.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.amsacode.predict4java.SatPassTime
import com.rtbishop.lookingsat.R
import java.text.SimpleDateFormat
import java.util.*

class SatPassAdapter(private val satPassList: MutableList<SatPassTime>) :
    RecyclerView.Adapter<SatPassAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return satPassList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.sat_pass_card, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val satPass = satPassList[position]

        val satName = holder.itemView.findViewById<TextView>(R.id.card_sat_name)
        val passVector = holder.itemView.findViewById<TextView>(R.id.card_pass_vector)
        val maxEl = holder.itemView.findViewById<TextView>(R.id.card_max_el)
        val passStart = holder.itemView.findViewById<TextView>(R.id.card_pass_start)
        val passEnd = holder.itemView.findViewById<TextView>(R.id.card_pass_end)

        satName.text = String.format("%s", "Meteor M2-2")
        passVector.text = String.format("V: %02d° -> %02d°", satPass.aosAzimuth, satPass.losAzimuth)
        maxEl.text = String.format("%.1f°", satPass.maxEl)
        passStart.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(satPass.startTime)
        passEnd.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(satPass.endTime)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        override fun onClick(v: View?) {}
    }
}