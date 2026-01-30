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
package com.rtbishop.look4sat.presentation.satellites

import com.rtbishop.look4sat.domain.model.SatItem

data class SatellitesState(
    val isDialogShown: Boolean,
    val isLoading: Boolean,
    val shouldSeeWarning: Boolean,
    val itemsList: List<SatItem>,
    val currentTypes: List<String>,
    val typesList: List<String>,
    val takeAction: (SatellitesAction) -> Unit
)

sealed class SatellitesAction {
    data object DismissWarning : SatellitesAction()
    data object SaveSelection : SatellitesAction()
    data class SearchFor(val query: String) : SatellitesAction()
    data object SelectAll : SatellitesAction()
    data class SelectSingle(val id: Int, val isTicked: Boolean) : SatellitesAction()
    data class SelectTypes(val types: List<String>) : SatellitesAction()
    data object ToggleTypesDialog : SatellitesAction()
    data object UnselectAll : SatellitesAction()
}
