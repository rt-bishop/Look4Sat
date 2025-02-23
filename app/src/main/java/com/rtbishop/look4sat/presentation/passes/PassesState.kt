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
