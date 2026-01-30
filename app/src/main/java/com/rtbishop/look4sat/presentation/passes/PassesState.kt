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
package com.rtbishop.look4sat.presentation.passes

import com.rtbishop.look4sat.domain.predict.OrbitalPass

data class PassesState(
    val isPassesDialogShown: Boolean,
    val isRadiosDialogShown: Boolean,
    val isRefreshing: Boolean,
    val isUtc: Boolean,
    val nextPass: OrbitalPass,
    val nextTime: String,
    val isNextTimeAos: Boolean,
    val hours: Int,
    val elevation: Double,
    val modes: List<String>,
    val itemsList: List<OrbitalPass>,
    val shouldSeeWelcome: Boolean,
    val takeAction: (PassesAction) -> Unit
)

sealed class PassesAction {
    data object DismissWelcome : PassesAction()
    data class FilterPasses(val hoursAhead: Int, val minElevation: Double) : PassesAction()
    data class FilterRadios(val modes: List<String>) : PassesAction()
    data object RefreshPasses : PassesAction()
    data object TogglePassesDialog : PassesAction()
    data object ToggleRadiosDialog : PassesAction()
}
