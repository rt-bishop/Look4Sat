package com.rtbishop.look4sat.presentation.satellites

import com.rtbishop.look4sat.domain.model.SatItem

data class SatellitesState(
    val isDialogShown: Boolean,
    val isLoading: Boolean,
    val itemsList: List<SatItem>,
    val currentType: String,
    val typesList: List<String>,
    val takeAction: (SatellitesAction) -> Unit
)

sealed class SatellitesAction {
    data object SaveSelection : SatellitesAction()
    data class SearchFor(val query: String) : SatellitesAction()
    data object SelectAll : SatellitesAction()
    data class SelectSingle(val id: Int, val isTicked: Boolean) : SatellitesAction()
    data class SelectType(val type: String) : SatellitesAction()
    data object ToggleTypesDialog : SatellitesAction()
    data object UnselectAll : SatellitesAction()
}
