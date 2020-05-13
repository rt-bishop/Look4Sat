package com.rtbishop.look4sat.utility

import android.content.Context
import android.widget.Toast
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

object GeneralUtils {

    private const val piDiv2 = Math.PI / 2.0

    fun getDateFor(value: Long): Date {
        return Date(value)
    }

    fun rad2Deg(value: Double): Double {
        return value * 180 / Math.PI
    }

    fun sph2CartX(azimuth: Double, elevation: Double, r: Double): Float {
        val radius = r * (piDiv2 - elevation) / piDiv2
        return (radius * cos(piDiv2 - azimuth)).toFloat()
    }

    fun sph2CartY(azimuth: Double, elevation: Double, r: Double): Float {
        val radius = r * (piDiv2 - elevation) / piDiv2
        return (radius * sin(piDiv2 - azimuth)).toFloat()
    }

    fun Any.toast(context: Context, duration: Int = Toast.LENGTH_SHORT): Toast {
        return Toast.makeText(context, this.toString(), duration).apply { show() }
    }
}