package com.rtbishop.look4sat.presentation.passes

import com.rtbishop.look4sat.domain.predict.OrbitalPass

data class PassesState(
    val isDialogShown: Boolean,
    val isRefreshing: Boolean,
    val isUtc: Boolean,
    val nextId: Int,
    val nextName: String,
    val nextTime: String,
    val hours: Int,
    val elevation: Double,
    val modes: List<String>,
    val itemsList: List<OrbitalPass>,
    val takeAction: (PassesAction) -> Unit
)

sealed class PassesAction {
    data class ApplyFilter(val hoursAhead: Int, val minElevation: Double, val modes: List<String>) : PassesAction()
    data object RefreshPasses : PassesAction()
    data object ToggleFilterDialog : PassesAction()
}
