/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.feature.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom osmdroid overlay that shades the night side of the globe.
 *
 * Works entirely in screen-pixel space: for each vertical strip on screen it
 * asks osmdroid for the geographic coordinate, then tests whether that point is
 * in the night half-sphere relative to the sub-solar point.  Because the
 * computation happens during draw() the result is always correct regardless
 * of zoom level or map scroll position — no polygon winding issues possible.
 *
 * A point (latRad, lonRad) is in night when the angle to the sub-solar point
 * exceeds 90°, i.e. the dot product of the two unit vectors is negative:
 *   dot = sin(lat)*sin(sunLat) + cos(lat)*cos(sunLat)*cos(lon - sunLon) < 0
 *
 * Performance: we sample one column per [stepPx] pixels (default 4) and draw
 * filled vertical rectangles.  On a 1080-wide screen this means ~270 trig
 * evaluations per row, which is imperceptible.
 */
class NightOverlay : Overlay() {

    /** Sub-solar latitude in degrees. */
    var sunLatDeg: Double = 0.0

    /** Sub-solar longitude in degrees. */
    var sunLonDeg: Double = 0.0

    private val nightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(75, 0, 0, 0)
    }

    private val rect = RectF()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        val proj = mapView.projection
        val sunLatRad = Math.toRadians(sunLatDeg)
        val sunLonRad = Math.toRadians(sunLonDeg)
        val sinSunLat = sin(sunLatRad)
        val cosSunLat = cos(sunLatRad)

        val w = mapView.width
        val h = mapView.height
        val stepPx = 4 // sample every N pixels — balance quality vs CPU

        // We scan column by column. For each column we determine the longitude,
        // then find the latitude range that is in night and shade it.
        // Since longitude is constant along a vertical strip and the day/night
        // boundary at a given longitude is at most two latitudes, we can do a
        // scan-line fill efficiently.

        var x = 0
        while (x < w) {
            // Get the geographic coordinate at the top and bottom of this column.
            val geoTop = proj.fromPixels(x, 0) ?: run { x += stepPx; continue }
            val geoBot = proj.fromPixels(x, h - 1) ?: run { x += stepPx; continue }

            val lonRad = Math.toRadians(geoTop.longitude)
            val cosLonDiff = cos(lonRad - sunLonRad)

            // Top pixel geographic lat
            val latTopRad = Math.toRadians(geoTop.latitude)
            // Bottom pixel geographic lat (osmdroid: y=0 is top of screen, higher y = lower lat)
            val latBotRad = Math.toRadians(geoBot.latitude)

            // dot(sunVec, pointVec) < 0 → night
            // dot = sin(lat)*sinSunLat + cos(lat)*cosSunLat*cosLonDiff
            val dotTop = sin(latTopRad) * sinSunLat + cos(latTopRad) * cosSunLat * cosLonDiff
            val dotBot = sin(latBotRad) * sinSunLat + cos(latBotRad) * cosSunLat * cosLonDiff

            when {
                dotTop < 0 && dotBot < 0 -> {
                    // Entire column is night — shade from top to bottom
                    rect.set(x.toFloat(), 0f, (x + stepPx).toFloat(), h.toFloat())
                    canvas.drawRect(rect, nightPaint)
                }

                dotTop >= 0 && dotBot >= 0 -> {
                    // Entire column is day — nothing to draw
                }

                else -> {
                    // Terminator crosses this column — find the crossing pixel by binary search
                    val crossY = findCrossingY(proj, x, 0, h - 1, sinSunLat, cosSunLat, cosLonDiff)
                    if (dotTop < 0) {
                        // Night at top, day at bottom
                        rect.set(x.toFloat(), 0f, (x + stepPx).toFloat(), crossY.toFloat())
                        canvas.drawRect(rect, nightPaint)
                    } else {
                        // Day at top, night at bottom
                        rect.set(x.toFloat(), crossY.toFloat(), (x + stepPx).toFloat(), h.toFloat())
                        canvas.drawRect(rect, nightPaint)
                    }
                }
            }
            x += stepPx
        }
    }

    /**
     * Binary-search for the pixel row where the day/night boundary crosses column [x].
     * [yTop] is in day, [yBot] is in night (or vice versa).
     */
    private fun findCrossingY(
        proj: org.osmdroid.views.Projection,
        x: Int,
        yTop: Int,
        yBot: Int,
        sinSunLat: Double,
        cosSunLat: Double,
        cosLonDiff: Double
    ): Int {
        var lo = yTop
        var hi = yBot
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            val geo = proj.fromPixels(x, mid) ?: return mid
            val latRad = Math.toRadians(geo.latitude)
            val dot = sin(latRad) * sinSunLat + cos(latRad) * cosSunLat * cosLonDiff
            if (dot < 0) hi = mid else lo = mid
        }
        return (lo + hi) / 2
    }
}
