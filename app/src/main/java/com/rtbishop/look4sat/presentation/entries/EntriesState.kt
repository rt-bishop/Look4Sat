package com.rtbishop.look4sat.presentation.entries

import com.rtbishop.look4sat.domain.model.SatItem

data class EntriesState(
    val isDialogShown: Boolean,
    val isLoading: Boolean,
    val itemsList: List<SatItem>,
    val currentType: String,
    val typesList: List<String>,
    val takeAction: (EntriesAction) -> Unit
)

sealed class EntriesAction {
    data object SaveSelection : EntriesAction()
    data class SearchFor(val query: String) : EntriesAction()
    data object SelectAll : EntriesAction()
    data class SelectSingle(val id: Int, val isTicked: Boolean) : EntriesAction()
    data class SelectType(val type: String) : EntriesAction()
    data object ToggleTypesDialog : EntriesAction()
    data object UnselectAll : EntriesAction()
}
