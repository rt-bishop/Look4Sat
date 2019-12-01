package com.rtbishop.lookingsat.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.lookingsat.R
import com.rtbishop.lookingsat.repo.Transmitter

class TransAdapter : RecyclerView.Adapter<TransAdapter.TransHolder>() {

    inner class TransHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private var transList = emptyList<Transmitter>()

    fun setList(list: List<Transmitter>) {
        transList = list
    }

    override fun getItemCount(): Int {
        return transList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.card_transmitter, parent, false)
        return TransHolder(itemView)
    }

    override fun onBindViewHolder(holder: TransHolder, position: Int) {
        val context = holder.itemView.context
        val freqDivider = 1000000f
        val transmitter = transList[position]
        val description = holder.itemView.findViewById<TextView>(R.id.trans_description)
        val downlink = holder.itemView.findViewById<TextView>(R.id.trans_downlink)
        val uplink = holder.itemView.findViewById<TextView>(R.id.trans_uplink)
        val mode = holder.itemView.findViewById<TextView>(R.id.trans_mode)
        val inverted = holder.itemView.findViewById<TextView>(R.id.trans_inverted)

        description.text = transmitter.description

        if (transmitter.downlinkLow != null && transmitter.downlinkHigh == null) {
            downlink.text = String.format(
                context.getString(R.string.pattern_trans_downlink_low),
                transmitter.downlinkLow / freqDivider
            )
        } else if (transmitter.downlinkLow != null && transmitter.downlinkHigh != null) {
            downlink.text = String.format(
                context.getString(R.string.pattern_trans_downlink_lowHigh),
                transmitter.downlinkLow / freqDivider,
                transmitter.downlinkHigh / freqDivider
            )
        } else {
            downlink.text = context.getString(R.string.no_downlink)
        }

        if (transmitter.uplinkLow != null && transmitter.uplinkHigh == null) {
            uplink.text = String.format(
                context.getString(R.string.pattern_trans_uplink_low),
                transmitter.uplinkLow / freqDivider
            )
        } else if (transmitter.uplinkLow != null && transmitter.uplinkHigh != null) {
            uplink.text = String.format(
                context.getString(R.string.pattern_trans_uplink_lowHigh),
                transmitter.uplinkLow / freqDivider,
                transmitter.uplinkHigh / freqDivider
            )
        } else {
            uplink.text = context.getString(R.string.no_uplink)
        }

        if (transmitter.mode != null) {
            mode.text =
                String.format(
                    context.getString(R.string.pattern_trans_mode),
                    transmitter.mode
                )
        } else {
            mode.text = context.getString(R.string.no_mode)
        }
        if (transmitter.isInverted) {
            inverted.text = String.format(
                context.getString(R.string.pattern_trans_inverted),
                context.getString(R.string.yes)
            )
        } else {
            inverted.text = String.format(
                context.getString(R.string.pattern_trans_inverted),
                context.getString(R.string.no)
            )
        }
    }
}