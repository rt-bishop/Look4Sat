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
package com.rtbishop.look4sat.domain.model

data class DatabaseState(
    val numberOfRadios: Int,
    val numberOfSatellites: Int,
    val updateTimestamp: Long
)

data class PassesSettings(
    val hoursAhead: Int,
    val minElevation: Double,
    val selectedModes: List<String>
)

data class RCSettings(
    val rotatorState: Boolean,
    val rotatorAddress: String,
    val rotatorPort: String,
    val bluetoothState: Boolean,
    val bluetoothFormat: String,
    val bluetoothName: String,
    val bluetoothAddress: String,
)

data class OtherSettings(
    val stateOfAutoUpdate: Boolean,
    val stateOfSensors: Boolean,
    val stateOfSweep: Boolean,
    val stateOfUtc: Boolean,
    val stateOfLightTheme: Boolean,
    val shouldSeeWarning: Boolean,
    val shouldSeeWelcome: Boolean
)

data class DataSourcesSettings(
    val useCustomTLE: Boolean,
    val useCustomTransceivers: Boolean,
    val tleUrl: String,
    val transceiversUrl: String
)
