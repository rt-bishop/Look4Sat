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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.repo.SatPass
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
            .inflate(R.layout.card_pass, parent, false)
        return PassHolder(itemView)
    }

    override fun onBindViewHolder(holder: PassHolder, position: Int) {
        val satPass = satPassList[position]

        val satName = holder.itemView.findViewById<TextView>(R.id.pass_satName)
        val satId = holder.itemView.findViewById<TextView>(R.id.pass_satId)
        val maxEl = holder.itemView.findViewById<TextView>(R.id.pass_maxEl)
        val passVector = holder.itemView.findViewById<TextView>(R.id.pass_azVector)
        val passStart = holder.itemView.findViewById<TextView>(R.id.pass_aosTime)
        val passEnd = holder.itemView.findViewById<TextView>(R.id.pass_losTime)
        val progressBar = holder.itemView.findViewById<ProgressBar>(R.id.pass_progress)

        val aosTime =
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(satPass.pass.startTime)
        val losTime =
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(satPass.pass.endTime)
        satName.text = satPass.tle.name
        satId.text = String.format("Id: %d", satPass.tle.catnum)
        maxEl.text = String.format("MaxEl: %.1f°", satPass.pass.maxEl)
        passVector.text =
            String.format("Az: %2d° -> %2d°", satPass.pass.aosAzimuth, satPass.pass.losAzimuth)
        passStart.text = String.format("AOS - %s", aosTime)
        passEnd.text = String.format("LOS - %s", losTime)

        holder.itemView.setOnClickListener {
            val action = SkyFragmentDirections.actionNavSkyToNavRadar(satPass)
            holder.itemView.findNavController().navigate(action)
        }
    }
}