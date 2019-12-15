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

class SatPassAdapter : RecyclerView.Adapter<SatPassAdapter.PassHolder>() {

    inner class PassHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private var satPassList = emptyList<SatPass>()

    fun setList(list: List<SatPass>) {
        satPassList = list
    }

    override fun getItemCount(): Int {
        return satPassList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PassHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.sat_pass_card, parent, false)
        return PassHolder(itemView)
    }

    override fun onBindViewHolder(holder: PassHolder, position: Int) {
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
}