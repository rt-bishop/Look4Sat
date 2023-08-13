package com.rtbishop.look4sat.presentation.entries

import com.rtbishop.look4sat.domain.model.SatItem

data class EntriesUiState(
    val isLoading: Boolean,
    val itemsList: List<SatItem>,
    val currentType: String,
    val typesList: List<String>,
    val takeAction: (EntriesUiAction) -> Unit
)

sealed class EntriesUiAction {
    data object SaveSelection : EntriesUiAction()
    data class SearchFor(val query: String) : EntriesUiAction()
    data object SelectAll : EntriesUiAction()
    data class SelectSingle(val id: Int, val isTicked: Boolean) : EntriesUiAction()
    data class SelectType(val type: String) : EntriesUiAction()
    data object UnselectAll : EntriesUiAction()
}
