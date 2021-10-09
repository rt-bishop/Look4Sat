/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.domain

object Constants {

    const val URL_BASE = "https://db.satnogs.org/api/"
    const val URL_CELESTRAK = "https://celestrak.com/NORAD/elements/active.txt"
    const val URL_AMSAT = "https://amsat.org/tle/current/nasabare.txt"
    const val URL_PRISM_CLASSFD = "https://www.prismnet.com/~mmccants/tles/classfd.zip"
    const val URL_PRISM_INTEL = "https://www.prismnet.com/~mmccants/tles/inttles.zip"
}
