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
package com.rtbishop.look4sat.domain.source

object Sources {
    const val RADIO_DATA_URL = "https://db.satnogs.org/api/transmitters/?format=json&status=active"
    val satelliteDataUrls = mapOf(
        "All" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=active&FORMAT=csv",
        "Amateur" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=amateur&FORMAT=csv",
        "Brightest" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=visual&FORMAT=csv",
        "Classified" to "https://www.mmccants.org/tles/classfd.zip",
        "Cubesat" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=cubesat&FORMAT=csv",
        "Education" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=education&FORMAT=csv",
        "Engineer" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=engineering&FORMAT=csv",
        "Geostationary" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=geo&FORMAT=csv",
        "Globalstar" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=globalstar&FORMAT=csv",
        "GNSS" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=gnss&FORMAT=csv",
        "Intelsat" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=intelsat&FORMAT=csv",
        "Iridium" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=iridium-NEXT&FORMAT=csv",
        "McCants" to "https://www.mmccants.org/tles/inttles.zip",
        "Military" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=military&FORMAT=csv",
        "New" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=last-30-days&FORMAT=csv",
        "OneWeb" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=oneweb&FORMAT=csv",
        "Orbcomm" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=orbcomm&FORMAT=csv",
        "R4UAB" to "https://r4uab.ru/satonline.txt",
        "Resource" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=resource&FORMAT=csv",
        "SatNOGS" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=satnogs&FORMAT=csv",
        "Science" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=science&FORMAT=csv",
        "Spire" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=spire&FORMAT=csv",
        "Starlink" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=starlink&FORMAT=csv",
        "Swarm" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=swarm&FORMAT=csv",
        "Weather" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=weather&FORMAT=csv",
        "X-Comm" to "https://celestrak.org/NORAD/elements/gp.php?GROUP=x-comm&FORMAT=csv",
        "Amsat" to "https://amsat.org/tle/current/nasabare.txt",
        "Other" to "" // key for sats filter
    )
}
